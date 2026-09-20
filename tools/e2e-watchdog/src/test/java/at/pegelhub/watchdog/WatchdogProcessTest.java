package at.pegelhub.watchdog;

import at.pegelhub.watchdog.config.WatchdogConfig;
import at.pegelhub.watchdog.lab.TrapReceiver;
import at.pegelhub.watchdog.state.StateStore;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static at.pegelhub.watchdog.WatchdogFixtures.BACK;
import static at.pegelhub.watchdog.WatchdogFixtures.config;
import static org.junit.jupiter.api.Assertions.*;

class WatchdogProcessTest {
    @TempDir
    Path directory;

    @Test
    @Timeout(25)
    void mainPublishesReadFailuresAndRecoveryThenClosesStateAfterAnInflightRead() throws Exception {
        var healthy = new AtomicBoolean();
        var blockRead = new AtomicBoolean();
        var readEntered = new CountDownLatch(1);
        var releaseRead = new CountDownLatch(1);
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        Process process = null;
        try (var receiver = new TrapReceiver(0)) {
            server.createContext("/token", exchange -> respond(exchange, 200,
                    "{\"access_token\":\"test-token\",\"expires_in\":3600}"));
            server.createContext("/api/v1/time-series/" + BACK + "/measurements", exchange -> {
                if (blockRead.get()) {
                    readEntered.countDown();
                    try {
                        releaseRead.await();
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        exchange.close();
                        return;
                    }
                }
                if (healthy.get()) {
                    respond(exchange, 200, """
                            {"timeSeriesId":"%s","representation":"canonical","unit":"cm","truncated":false,
                             "measurements":[{"observedAt":"%s","value":281.0}]}
                            """.formatted(BACK, Instant.now()));
                } else {
                    respond(exchange, 503, "Core is unavailable");
                }
            });
            server.start();

            var base = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
            var config = new WatchdogConfig("test",
                    new WatchdogConfig.Core(base, base + "token", "test", "secret"),
                    BACK, 600, 1L, config(receiver.port(), "v2c").snmp());
            var configFile = directory.resolve("watchdog.yaml");
            Files.writeString(configFile, new YAMLMapper().writeValueAsString(config));
            Files.writeString(directory.resolve("secret"), "test-secret");
            Files.writeString(directory.resolve("community"), "test-community");
            var stateDirectory = directory.resolve("state");
            var command = new ProcessBuilder(
                    Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                    "-cp", System.getProperty("java.class.path"), WatchdogApplication.class.getName(),
                    "run", configFile.toString());
            command.environment().put("WATCHDOG_STATE_DIR", stateDirectory.toString());
            process = command.redirectErrorStream(true)
                    .redirectOutput(directory.resolve("process.log").toFile()).start();

            var error = receiver.await();
            assertNotNull(error, "A failed read must still reach publication in the same cycle");
            assertTrue(error.contains("status=ERR reason=read_failed"), error);
            assertNull(receiver.await(1_200), "Repeated failing cycles must not repeat ERR");

            healthy.set(true);
            var recovery = receiver.await();
            assertNotNull(recovery, "A successful fresh read must publish recovery");
            assertTrue(recovery.contains("status=OK reason=fresh"), recovery);
            assertNull(receiver.await(1_200), "Repeated healthy cycles must not repeat OK");
            assertEquals(true, StateStore.inspect(stateDirectory, Instant.now()).get("healthy"));

            blockRead.set(true);
            assertTrue(readEntered.await(3, TimeUnit.SECONDS));
            process.destroy();
            releaseRead.countDown();
            assertTrue(process.waitFor(5, TimeUnit.SECONDS), "Shutdown must finish the cycle and close resources");
            try (var reopened = new StateStore(stateDirectory, config)) {
                assertEquals("OK", reopened.publication(config.snmp().receivers().getFirst().id()).signal());
            }
        } finally {
            releaseRead.countDown();
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
            server.stop(0);
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        try (exchange) {
            exchange.getRequestBody().readAllBytes();
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
    }
}
