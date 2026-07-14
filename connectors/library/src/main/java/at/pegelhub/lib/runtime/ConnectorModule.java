package at.pegelhub.lib.runtime;

public interface ConnectorModule {
    String name();

    ConnectorRuntimeDefinition define(ConnectorBootstrap bootstrap) throws Exception;
}
