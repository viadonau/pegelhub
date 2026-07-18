package at.pegelhub.lib.runtime;

import java.time.Duration;
import java.util.List;

public final class ConnectorRuntimeDefinition {
    private final String name;
    private final int threadCount;
    private final Duration shutdownTimeout;
    private final List<StartAction> startActions;
    private final List<AutoCloseable> resources;
    private final List<ScheduledTask> tasks;

    ConnectorRuntimeDefinition(
            String name,
            int threadCount,
            Duration shutdownTimeout,
            List<StartAction> startActions,
            List<AutoCloseable> resources,
            List<ScheduledTask> tasks) {
        this.name = name;
        this.threadCount = threadCount;
        this.shutdownTimeout = shutdownTimeout;
        this.startActions = List.copyOf(startActions);
        this.resources = List.copyOf(resources);
        this.tasks = List.copyOf(tasks);
    }

    public String name() {
        return name;
    }

    int threadCount() {
        return threadCount;
    }

    Duration shutdownTimeout() {
        return shutdownTimeout;
    }

    List<StartAction> startActions() {
        return startActions;
    }

    List<AutoCloseable> resources() {
        return resources;
    }

    List<ScheduledTask> tasks() {
        return tasks;
    }

    @FunctionalInterface
    interface StartAction {
        void run() throws Exception;
    }

    record ScheduledTask(
            String name,
            Runnable runnable,
            Duration initialDelay,
            Duration delay
    ) {}
}
