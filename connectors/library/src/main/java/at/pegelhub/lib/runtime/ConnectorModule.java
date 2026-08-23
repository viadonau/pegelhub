package at.pegelhub.lib.runtime;

import at.pegelhub.lib.PegelHubClientFactory;
import at.pegelhub.lib.config.ConnectorConfigDirectory;

public interface ConnectorModule {
    String name();

    void validate(ConnectorConfigDirectory configDirectory) throws Exception;

    ConnectorRuntimeDefinition define(
            ConnectorConfigDirectory configDirectory,
            PegelHubClientFactory coreClients) throws Exception;
}
