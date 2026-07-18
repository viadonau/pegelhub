package at.pegelhub.connector.icc;

import at.pegelhub.connector.icc.config.IccConnectorConfig;
import at.pegelhub.connector.icc.config.IccConnectorConfigLoader;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.PegelHubClientFactory;
import at.pegelhub.lib.config.ConnectorConfigDirectory;
import at.pegelhub.lib.runtime.ConnectorModule;
import at.pegelhub.lib.runtime.ConnectorRuntimeAssembly;
import at.pegelhub.lib.runtime.ConnectorRuntimeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IccConnectorModule implements ConnectorModule {
    private static final Logger LOG = LoggerFactory.getLogger(IccConnectorModule.class);
    private final IccConnectorConfigLoader configLoader = new IccConnectorConfigLoader();

    @Override
    public String name() {
        return "ICC Connector";
    }

    @Override
    public ConnectorRuntimeDefinition define(
            ConnectorConfigDirectory configDirectory,
            PegelHubClientFactory coreClients) throws Exception {
        IccConnectorConfig config = configLoader.load(configDirectory);

        LOG.info("CoreUrl: {}", config.localCore().baseUrl());
        LOG.info("ExternalCoreUrl: {}", config.remoteCore().baseUrl());
        LOG.info("Mappings: {}", config.mappings());
        LOG.info("Interval: {}", config.pollInterval());

        try (ConnectorRuntimeAssembly runtime = ConnectorRuntimeAssembly.begin(name())) {
            PegelHubClient coreClient = runtime.own(coreClients.create(config.localCore()));
            PegelHubClient externalClient = runtime.own(coreClients.create(config.remoteCore()));

            runtime.fixedDelayTask(
                    "icc-sync",
                    new IccSynchronizer(coreClient, externalClient, config.mappings(), config.pollInterval()),
                    config.pollInterval());
            return runtime.complete();
        }
    }
}
