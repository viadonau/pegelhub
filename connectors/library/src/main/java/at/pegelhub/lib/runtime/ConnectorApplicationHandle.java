package at.pegelhub.lib.runtime;

public interface ConnectorApplicationHandle extends AutoCloseable {
    @Override
    void close();
}
