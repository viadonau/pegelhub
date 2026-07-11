package at.pegelhub.lib.runtime;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ConnectorPlan {
    private final String name;
    private final int threadCount;
    private final Duration shutdownTimeout;
    private final List<StartHook> startHooks;
    private final List<AutoCloseable> closeables;
    private final List<ScheduledTask> tasks;

    private ConnectorPlan(
            String name,
            int threadCount,
            Duration shutdownTimeout,
            List<StartHook> startHooks,
            List<AutoCloseable> closeables,
            List<ScheduledTask> tasks) {
        this.name = name;
        this.threadCount = threadCount;
        this.shutdownTimeout = shutdownTimeout;
        this.startHooks = List.copyOf(startHooks);
        this.closeables = List.copyOf(closeables);
        this.tasks = List.copyOf(tasks);
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    ConnectorRuntime toRuntime() {
        var runtime = ConnectorRuntime.builder(name)
                .threadCount(threadCount);
        if (shutdownTimeout != null) {
            runtime.shutdownTimeout(shutdownTimeout);
        }
        for (StartHook hook : startHooks) {
            runtime.onStart(hook::run);
        }
        for (ScheduledTask task : tasks) {
            runtime.fixedDelayTask(task.name(), task.runnable(), task.initialDelay(), task.delay());
        }
        for (AutoCloseable closeable : closeables) {
            runtime.closeOnStop(closeable);
        }
        return runtime.build();
    }

    String name() {
        return name;
    }

    public interface StartHook {
        void run() throws Exception;
    }

    public static final class Builder {
        private final String name;
        private final List<StartHook> startHooks = new ArrayList<>();
        private final List<AutoCloseable> closeables = new ArrayList<>();
        private final List<ScheduledTask> tasks = new ArrayList<>();
        private Duration shutdownTimeout;
        private int threadCount = 1;

        private Builder(String name) {
            this.name = requireText(name, "name");
        }

        public Builder threadCount(int threadCount) {
            if (threadCount < 1) {
                throw new IllegalArgumentException("threadCount must be at least 1");
            }
            this.threadCount = threadCount;
            return this;
        }

        public Builder shutdownTimeout(Duration shutdownTimeout) {
            this.shutdownTimeout = requirePositive(shutdownTimeout, "shutdownTimeout");
            return this;
        }

        public Builder onStart(StartHook hook) {
            startHooks.add(Objects.requireNonNull(hook, "hook"));
            return this;
        }

        public Builder closeOnStop(AutoCloseable closeable) {
            closeables.add(Objects.requireNonNull(closeable, "closeable"));
            return this;
        }

        public Builder fixedDelayTask(String taskName, Runnable task, Duration delay) {
            return fixedDelayTask(taskName, task, Duration.ZERO, delay);
        }

        public Builder fixedDelayTask(String taskName, Runnable task, Duration initialDelay, Duration delay) {
            tasks.add(new ScheduledTask(
                    requireText(taskName, "taskName"),
                    Objects.requireNonNull(task, "task"),
                    requireNonNegative(initialDelay, "initialDelay"),
                    requirePositive(delay, "delay")));
            return this;
        }

        public ConnectorPlan build() {
            return new ConnectorPlan(name, threadCount, shutdownTimeout, startHooks, closeables, tasks);
        }

        private static String requireText(String value, String name) {
            Objects.requireNonNull(value, name);
            if (value.isBlank()) {
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

    private record ScheduledTask(String name, Runnable runnable, Duration initialDelay, Duration delay) {
    }
}
