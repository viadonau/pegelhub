package at.pegelhub.lib.runtime;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ConnectorRuntimeAssembly implements AutoCloseable {
    private static final Duration DEFAULT_SHUTDOWN_TIMEOUT = Duration.ofSeconds(15);

    private final String name;
    private final List<ConnectorRuntimeDefinition.StartAction> startActions = new ArrayList<>();
    private final List<AutoCloseable> resources = new ArrayList<>();
    private final List<ConnectorRuntimeDefinition.ScheduledTask> tasks = new ArrayList<>();
    private Duration shutdownTimeout = DEFAULT_SHUTDOWN_TIMEOUT;
    private int threadCount = 1;
    private boolean completed;

    private ConnectorRuntimeAssembly(String name) {
        this.name = requireText(name, "name");
    }

    public static ConnectorRuntimeAssembly begin(String name) {
        return new ConnectorRuntimeAssembly(name);
    }

    public <T extends AutoCloseable> T own(T resource) {
        ensureOpen();
        resources.add(Objects.requireNonNull(resource, "resource"));
        return resource;
    }

    public ConnectorRuntimeAssembly threadCount(int threadCount) {
        ensureOpen();
        if (threadCount < 1) {
            throw new IllegalArgumentException("threadCount must be at least 1");
        }
        this.threadCount = threadCount;
        return this;
    }

    public ConnectorRuntimeAssembly shutdownTimeout(Duration shutdownTimeout) {
        ensureOpen();
        this.shutdownTimeout = requirePositive(shutdownTimeout, "shutdownTimeout");
        return this;
    }

    public ConnectorRuntimeAssembly onStart(StartAction action) {
        ensureOpen();
        Objects.requireNonNull(action, "action");
        startActions.add(action::run);
        return this;
    }

    public ConnectorRuntimeAssembly fixedDelayTask(String taskName, Runnable task, Duration delay) {
        return fixedDelayTask(taskName, task, Duration.ZERO, delay);
    }

    public ConnectorRuntimeAssembly fixedDelayTask(
            String taskName,
            Runnable task,
            Duration initialDelay,
            Duration delay) {
        ensureOpen();
        tasks.add(new ConnectorRuntimeDefinition.ScheduledTask(
                requireText(taskName, "taskName"),
                Objects.requireNonNull(task, "task"),
                requireNonNegative(initialDelay, "initialDelay"),
                requirePositive(delay, "delay")));
        return this;
    }

    public ConnectorRuntimeDefinition complete() {
        ensureOpen();
        completed = true;
        return new ConnectorRuntimeDefinition(
                name,
                threadCount,
                shutdownTimeout,
                startActions,
                resources,
                tasks);
    }

    @Override
    public void close() {
        if (completed) {
            return;
        }
        ConnectorResourceCloser.closeInReverse(name + " assembly", resources);
    }

    private void ensureOpen() {
        if (completed) {
            throw new IllegalStateException(name + " runtime assembly is already complete");
        }
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

    @FunctionalInterface
    public interface StartAction {
        void run() throws Exception;
    }
}
