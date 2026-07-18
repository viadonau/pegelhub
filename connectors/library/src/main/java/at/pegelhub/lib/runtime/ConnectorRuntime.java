package at.pegelhub.lib.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

final class ConnectorRuntime implements ConnectorApplicationHandle {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectorRuntime.class);
    private static final Duration DEFAULT_SHUTDOWN_TIMEOUT = Duration.ofSeconds(15);

    private final String name;
    private final ScheduledExecutorService scheduler;
    private final List<ScheduledConnectorTask> tasks;
    private final List<CheckedRunnable> startHooks;
    private final List<AutoCloseable> closeables;
    private final Duration shutdownTimeout;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    private ConnectorRuntime(
            String name,
            int threadCount,
            List<ScheduledConnectorTask> tasks,
            List<CheckedRunnable> startHooks,
            List<AutoCloseable> closeables,
            Duration shutdownTimeout) {
        this.name = name;
        this.scheduler = Executors.newScheduledThreadPool(threadCount, new ConnectorThreadFactory(name));
        this.tasks = List.copyOf(tasks);
        this.startHooks = List.copyOf(startHooks);
        this.closeables = List.copyOf(closeables);
        this.shutdownTimeout = shutdownTimeout;
    }

    static Builder builder(String name) {
        return new Builder(name);
    }

    synchronized void start() {
        if (started.get()) {
            return;
        }

        if (stopped.get()) {
            throw new IllegalStateException(name + " runtime has already been stopped");
        }

        try {
            for (CheckedRunnable hook : startHooks) {
                runStartHook(hook);
            }

            for (ScheduledConnectorTask task : tasks) {
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

        List<AutoCloseable> reversed = new ArrayList<>(closeables);
        Collections.reverse(reversed);
        for (AutoCloseable closeable : reversed) {
            closeQuietly(closeable);
        }

        LOG.info("{} runtime stopped.", name);
    }

    @Override
    public void close() {
        stop();
    }

    private void runStartHook(CheckedRunnable hook) {
        try {
            hook.run();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start " + name + " runtime", e);
        }
    }

    private Runnable guarded(ScheduledConnectorTask task) {
        return () -> {
            try {
                task.runnable().run();
            } catch (Exception e) {
                LOG.error("{} task '{}' failed. The runtime will keep scheduling it.", name, task.name(), e);
            }
        };
    }

    private void closeQuietly(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
            LOG.error("{} runtime close hook failed.", name, e);
        }
    }

    interface CheckedRunnable {
        void run() throws Exception;
    }

    static final class Builder {
        private final String name;
        private final List<ScheduledConnectorTask> tasks = new ArrayList<>();
        private final List<CheckedRunnable> startHooks = new ArrayList<>();
        private final List<AutoCloseable> closeables = new ArrayList<>();
        private Duration shutdownTimeout = DEFAULT_SHUTDOWN_TIMEOUT;
        private int threadCount = 1;

        private Builder(String name) {
            this.name = requireText(name, "name");
        }

        Builder threadCount(int threadCount) {
            if (threadCount < 1) {
                throw new IllegalArgumentException("threadCount must be at least 1");
            }
            this.threadCount = threadCount;
            return this;
        }

        Builder shutdownTimeout(Duration shutdownTimeout) {
            this.shutdownTimeout = requirePositive(shutdownTimeout, "shutdownTimeout");
            return this;
        }

        Builder onStart(CheckedRunnable hook) {
            startHooks.add(Objects.requireNonNull(hook, "hook"));
            return this;
        }

        Builder closeOnStop(AutoCloseable closeable) {
            closeables.add(Objects.requireNonNull(closeable, "closeable"));
            return this;
        }

        Builder fixedDelayTask(String taskName, Runnable task, Duration delay) {
            return fixedDelayTask(taskName, task, Duration.ZERO, delay);
        }

        Builder fixedDelayTask(String taskName, Runnable task, Duration initialDelay, Duration delay) {
            tasks.add(new ScheduledConnectorTask(
                    requireText(taskName, "taskName"),
                    Objects.requireNonNull(task, "task"),
                    requireNonNegative(initialDelay, "initialDelay"),
                    requirePositive(delay, "delay")));
            return this;
        }

        ConnectorRuntime build() {
            return new ConnectorRuntime(name, threadCount, tasks, startHooks, closeables, shutdownTimeout);
        }

        private static String requireText(String value, String name) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
            return value;
        }

        private static Duration requirePositive(Duration value, String name) {
            if (value == null || value.isZero() || value.isNegative()) {
                throw new IllegalArgumentException(name + " must be positive");
            }
            return value;
        }

        private static Duration requireNonNegative(Duration value, String name) {
            if (value == null || value.isNegative()) {
                throw new IllegalArgumentException(name + " must not be negative");
            }
            return value;
        }
    }

    private record ScheduledConnectorTask(String name, Runnable runnable, Duration initialDelay, Duration delay) {
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
