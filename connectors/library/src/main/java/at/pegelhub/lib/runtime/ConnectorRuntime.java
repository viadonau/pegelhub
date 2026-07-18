package at.pegelhub.lib.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class ConnectorRuntime implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectorRuntime.class);

    private final String name;
    private final ScheduledExecutorService scheduler;
    private final List<ConnectorRuntimeDefinition.ScheduledTask> tasks;
    private final List<ConnectorRuntimeDefinition.StartAction> startActions;
    private final List<AutoCloseable> resources;
    private final Duration shutdownTimeout;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    private ConnectorRuntime(ConnectorRuntimeDefinition definition) {
        this.name = definition.name();
        this.scheduler = Executors.newScheduledThreadPool(
                definition.threadCount(),
                new ConnectorThreadFactory(name));
        this.tasks = definition.tasks();
        this.startActions = definition.startActions();
        this.resources = definition.resources();
        this.shutdownTimeout = definition.shutdownTimeout();
    }

    static ConnectorRuntime start(ConnectorRuntimeDefinition definition) {
        ConnectorRuntime runtime;
        try {
            runtime = new ConnectorRuntime(definition);
        } catch (RuntimeException | Error e) {
            ConnectorResourceCloser.closeInReverse(definition.name() + " runtime", definition.resources());
            throw e;
        }
        runtime.startInternal();
        return runtime;
    }

    private synchronized void startInternal() {
        if (started.get()) {
            return;
        }
        if (stopped.get()) {
            throw new IllegalStateException(name + " runtime has already been stopped");
        }

        try {
            for (ConnectorRuntimeDefinition.StartAction action : startActions) {
                runStartAction(action);
            }
            for (ConnectorRuntimeDefinition.ScheduledTask task : tasks) {
                scheduler.scheduleWithFixedDelay(
                        guarded(task),
                        task.initialDelay().toMillis(),
                        task.delay().toMillis(),
                        TimeUnit.MILLISECONDS);
            }
            started.set(true);
            LOG.info("{} runtime started with {} scheduled task(s).", name, tasks.size());
        } catch (RuntimeException | Error e) {
            stop();
            throw e;
        }
    }

    Thread addShutdownHook() {
        Thread hook = new Thread(this::stop, name + "-shutdown");
        Runtime.getRuntime().addShutdownHook(hook);
        return hook;
    }

    void stop() {
        if (!stopped.compareAndSet(false, true)) {
            return;
        }

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(shutdownTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                scheduler.shutdownNow();
                if (!scheduler.awaitTermination(shutdownTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    LOG.warn("{} runtime tasks did not stop within the shutdown timeout.", name);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
        }

        ConnectorResourceCloser.closeInReverse(name + " runtime", resources);
        LOG.info("{} runtime stopped.", name);
    }

    @Override
    public void close() {
        stop();
    }

    private void runStartAction(ConnectorRuntimeDefinition.StartAction action) {
        try {
            action.run();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start " + name + " runtime", e);
        }
    }

    private Runnable guarded(ConnectorRuntimeDefinition.ScheduledTask task) {
        return () -> {
            try {
                task.runnable().run();
            } catch (Exception e) {
                LOG.error("{} task '{}' failed. The runtime will keep scheduling it.", name, task.name(), e);
            }
        };
    }

    private static final class ConnectorThreadFactory implements java.util.concurrent.ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger();
        private final String prefix;

        private ConnectorThreadFactory(String name) {
            this.prefix = name.toLowerCase().replaceAll("[^a-z0-9]+", "-");
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, prefix + "-runtime-" + counter.incrementAndGet());
            thread.setDaemon(false);
            return thread;
        }
    }
}
