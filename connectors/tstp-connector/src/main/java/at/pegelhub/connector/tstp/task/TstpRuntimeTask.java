package at.pegelhub.connector.tstp.task;

public record TstpRuntimeTask(
        Runnable task,
        AutoCloseable closeable
) {}
