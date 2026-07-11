package at.pegelhub.lib.runtime;

public interface ConnectorModule {
    String name();

    ConnectorPlan plan(ConnectorContext context) throws Exception;
}
