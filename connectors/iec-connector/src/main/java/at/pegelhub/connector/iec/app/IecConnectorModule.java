package at.pegelhub.connector.iec.app;

import at.pegelhub.connector.iec.config.IecConnectorConfig;
import at.pegelhub.connector.iec.config.IecConnectorConfigLoader;
import at.pegelhub.connector.iec.datapoints.IecMappingIndex;
import at.pegelhub.connector.iec.iec.IecClient;
import at.pegelhub.connector.iec.iec.impl.IecClientImpl;
import at.pegelhub.connector.iec.jobs.CoreToIecJob;
import at.pegelhub.connector.iec.jobs.IecToCoreJob;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.PegelHubClientFactory;
import at.pegelhub.lib.config.ConnectorConfigDirectory;
import at.pegelhub.lib.runtime.ConnectorModule;
import at.pegelhub.lib.runtime.ConnectorRuntimeAssembly;
import at.pegelhub.lib.runtime.ConnectorRuntimeDefinition;

import java.net.InetAddress;
import java.time.Duration;

public final class IecConnectorModule implements ConnectorModule {
    private final IecConnectorConfigLoader configLoader = new IecConnectorConfigLoader();

    @Override
    public String name() {
        return "IEC Connector";
    }

    @Override
    public ConnectorRuntimeDefinition define(
            ConnectorConfigDirectory configDirectory,
            PegelHubClientFactory coreClients) throws Exception {
        IecConnectorConfig config = configLoader.load(configDirectory);
        IecMappingIndex mappingIndex = new IecMappingIndex(config.mappings());

        try (ConnectorRuntimeAssembly runtime = ConnectorRuntimeAssembly.begin(name())) {
            PegelHubClient client = runtime.own(coreClients.create(config.coreConnection()));

            IecClient iecClient = new IecClientImpl(
                    InetAddress.getByName(config.server().host()),
                    config.server().port(),
                    config.server().commonAddress(),
                    mappingIndex.protocolToCoreIoas());
            runtime.own(iecClient::disconnect);

            runtime
                    .threadCount(2)
                    .onStart(iecClient::connect)
                    .fixedDelayTask("iec-to-core", new IecToCoreJob(iecClient, mappingIndex, client),
                            Duration.ofSeconds(1), config.pollInterval())
                    .fixedDelayTask("core-to-iec", new CoreToIecJob(iecClient, mappingIndex, client),
                            Duration.ofSeconds(1), config.pollInterval());
            return runtime.complete();
        }
    }
}
