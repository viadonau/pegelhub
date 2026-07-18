package at.pegelhub.connector.tstp;

import at.pegelhub.connector.tstp.catalog.TstpCatalogResolver;
import at.pegelhub.connector.tstp.client.HttpTstpClient;
import at.pegelhub.connector.tstp.client.TstpClient;
import at.pegelhub.connector.tstp.codec.TstpBinaryCodec;
import at.pegelhub.connector.tstp.codec.TstpXmlCodec;
import at.pegelhub.connector.tstp.config.TstpConnectorConfig;
import at.pegelhub.connector.tstp.config.TstpConnectorConfigLoader;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.PegelHubClientFactory;
import at.pegelhub.lib.config.ConnectorConfigDirectory;
import at.pegelhub.lib.runtime.ConnectorModule;
import at.pegelhub.lib.runtime.ConnectorRuntimeAssembly;
import at.pegelhub.lib.runtime.ConnectorRuntimeDefinition;

public final class TstpConnectorModule implements ConnectorModule {
    private final TstpConnectorConfigLoader configLoader = new TstpConnectorConfigLoader();

    @Override
    public String name() {
        return "TSTP Connector";
    }

    @Override
    public ConnectorRuntimeDefinition define(
            ConnectorConfigDirectory configDirectory,
            PegelHubClientFactory coreClients) throws Exception {
        TstpConnectorConfig config = configLoader.load(configDirectory);

        try (ConnectorRuntimeAssembly runtime = ConnectorRuntimeAssembly.begin(name())) {
            PegelHubClient coreClient = runtime.own(coreClients.create(config.coreConnection()));
            TstpClient tstpClient = runtime.own(HttpTstpClient.open(
                    config.server().host(),
                    config.server().port(),
                    new TstpXmlCodec(new TstpBinaryCodec())));

            TstpSynchronizer synchronizer = new TstpSynchronizer(
                    coreClient,
                    tstpClient,
                    new TstpCatalogResolver(tstpClient),
                    config.mappings(),
                    config.pollInterval());

            runtime.fixedDelayTask("tstp-sync", synchronizer, config.pollInterval());

            return runtime.complete();
        }
    }
}
