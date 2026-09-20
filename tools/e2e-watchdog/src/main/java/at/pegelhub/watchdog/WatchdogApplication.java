package at.pegelhub.watchdog;

import at.pegelhub.lib.CoreClientOptions;
import at.pegelhub.lib.PegelHubClientFactory;
import at.pegelhub.watchdog.config.ConfigurationFailure;
import at.pegelhub.watchdog.config.WatchdogConfig;
import at.pegelhub.watchdog.monitoring.Watchdog;
import at.pegelhub.watchdog.snmp.SnmpPublisher;
import at.pegelhub.watchdog.state.StateStore;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * No server: status and health are local, read-only commands suitable for docker exec and HEALTHCHECK.
 */
public final class WatchdogApplication {
    private static final CoreClientOptions CLIENT_OPTIONS =
            new CoreClientOptions(Duration.ofSeconds(5), Duration.ofSeconds(10), true);
    private static final long HEARTBEAT_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(30);

    private WatchdogApplication() {
    }

    public static void main(String[] args) {
        try {
            runCommand(args);
        } catch (Exception failure) {
            reportFailure(args, failure);
            System.exit(1);
        }
    }

    private static void runCommand(String[] args) throws Exception {
        String command = args.length == 0 ? "run" : args[0];
        Path stateDirectory = Path.of(
                System.getenv().getOrDefault("WATCHDOG_STATE_DIR", "/var/lib/pegelhub-watchdog"));
        switch (command) {
            case "status", "health" -> inspectState(command, stateDirectory);
            case "run" -> {
                Path configFile = Path.of(args.length > 1 ? args[1] : "/app/config/watchdog.yaml").toAbsolutePath();
                run(configFile, stateDirectory);
            }
            default -> throw new IllegalArgumentException("Use run [config], status, or health");
        }
    }

    private static void inspectState(String command, Path stateDirectory) throws Exception {
        var snapshot = StateStore.inspect(stateDirectory, Instant.now());
        if (command.equals("status")) {
            System.out.println(Json.CODEC.toJson(snapshot));
        }
        if (command.equals("health") && !Boolean.TRUE.equals(snapshot.get("healthy"))) {
            System.exit(1);
        }
    }

    private static void reportFailure(String[] args, Exception failure) {
        // Exception chains can include HTTP bodies, URLs, YAML excerpts, and resolved credentials.
        String diagnostic = failure instanceof ConfigurationFailure ? failure.getMessage()
                : "Watchdog failed (" + failure.getClass().getSimpleName()
                  + "); check configuration, permissions, and state availability";
        System.err.println(diagnostic);
        if (args.length > 0 && args[0].equals("status")) {
            System.out.println(Json.CODEC.toJson(Map.of("schemaVersion", 2, "healthy", false, "error", diagnostic)));
        }
    }

    private static void run(Path configFile, Path stateDirectory) throws Exception {
        var stopRequested = new CountDownLatch(1);
        var shutdownComplete = new CountDownLatch(1);
        Thread shutdownHook = createShutdownHook(stopRequested, shutdownComplete);
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        try {
            runUntilStopped(configFile, stateDirectory, stopRequested);
        } finally {
            shutdownComplete.countDown();
            removeShutdownHook(shutdownHook);
        }
    }

    private static Thread createShutdownHook(CountDownLatch stopRequested, CountDownLatch shutdownComplete) {
        // Merely signaling main is insufficient: the JVM can exit as soon as shutdown hooks return.
        // Keep this hook alive until main has finished the cycle and closed clients and local state.
        return new Thread(() -> {
            stopRequested.countDown();
            try {
                shutdownComplete.await(40, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }, "watchdog-shutdown");
    }

    private static void removeShutdownHook(Thread shutdownHook) {
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException stopping) {
            // JVM shutdown already owns the hook; it no longer needs to be unregistered.
        }
    }

    private static void runUntilStopped(Path configFile, Path stateDirectory, CountDownLatch stopRequested)
            throws Exception {
        WatchdogConfig config = WatchdogConfig.load(configFile);
        var connection = config.connection(configFile.getParent());
        var clients = PegelHubClientFactory.http(CLIENT_OPTIONS);

        try (var store = new StateStore(stateDirectory, config);
             var coreClient = clients.create(connection)) {
            store.beginSession(Instant.now());
            try (var publisher = new SnmpPublisher(config, configFile.getParent(), store)) {
                var watchdog = new Watchdog(config, store, coreClient, Clock.systemUTC());
                var cycle = withHeartbeat(
                        () -> {
                            watchdog.checkFreshness();
                            publisher.publish(Instant.now());
                        },
                        () -> store.progress(Instant.now()),
                        System::nanoTime);
                runLoop(cycle, config.pollSeconds(), stopRequested);
            }
        }
    }

    /**
     * Run on the calling thread; unexpected failures propagate to main and stop the process.
     */
    static void runLoop(Runnable cycle, long pollSeconds, CountDownLatch stopRequested) throws InterruptedException {
        while (stopRequested.getCount() != 0) {
            cycle.run();
            stopRequested.await(pollSeconds, TimeUnit.SECONDS);
        }
    }

    /**
     * Record completed cycles, not route health; a blocked read or send cannot renew progress.
     */
    static Runnable withHeartbeat(Runnable operation, Runnable heartbeat, LongSupplier nanoTime) {
        return new Runnable() {
            private long lastHeartbeat = nanoTime.getAsLong();

            @Override
            public void run() {
                operation.run();
                long now = nanoTime.getAsLong();
                if (now - lastHeartbeat >= HEARTBEAT_INTERVAL_NANOS) {
                    heartbeat.run();
                    lastHeartbeat = now;
                }
            }
        };
    }
}
