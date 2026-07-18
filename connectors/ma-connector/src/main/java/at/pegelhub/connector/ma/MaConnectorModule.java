package at.pegelhub.connector.ma;

import at.pegelhub.connector.ma.config.MaConnectorConfig;
import at.pegelhub.connector.ma.config.MaConnectorConfigLoader;
import at.pegelhub.connector.ma.core.MaInputMappingIndex;
import at.pegelhub.connector.ma.core.MaInputPollingJob;
import at.pegelhub.connector.ma.jni.RevPiReader;
import at.pegelhub.connector.ma.jni.RevPiReaderImpl;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.PegelHubClientFactory;
import at.pegelhub.lib.config.ConnectorConfigDirectory;
import at.pegelhub.lib.runtime.ConnectorModule;
import at.pegelhub.lib.runtime.ConnectorRuntimeAssembly;
import at.pegelhub.lib.runtime.ConnectorRuntimeDefinition;

import java.time.Duration;

public final class MaConnectorModule implements ConnectorModule {
    private final MaConnectorConfigLoader configLoader = new MaConnectorConfigLoader();

    @Override
    public String name() {
        return "mA Connector";
    }

    @Override
    public ConnectorRuntimeDefinition define(
            ConnectorConfigDirectory configDirectory,
            PegelHubClientFactory coreClients) throws Exception {
        MaConnectorConfig config = configLoader.load(configDirectory);

        try (ConnectorRuntimeAssembly runtime = ConnectorRuntimeAssembly.begin(name())) {
            PegelHubClient client = runtime.own(coreClients.create(config.coreConnection()));

            RevPiReader revPiReader = new RevPiReaderImpl();
            runtime.own(revPiReader::close);
            MaInputMappingIndex mappingIndex = new MaInputMappingIndex(revPiReader, config.mappings());
            MaInputPollingJob pollingJob = new MaInputPollingJob(mappingIndex, revPiReader, client);

            runtime.onStart(mappingIndex::loadInputs)
                    .fixedDelayTask("ma-input-poll", pollingJob,
                            Duration.ofSeconds(1), config.pollInterval());
            return runtime.complete();
        }
    }
}
