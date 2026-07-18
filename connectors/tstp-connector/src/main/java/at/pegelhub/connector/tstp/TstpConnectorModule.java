package at.pegelhub.connector.tstp;

import at.pegelhub.connector.tstp.config.TstpConnectorConfig;
import at.pegelhub.connector.tstp.config.TstpConnectorConfigLoader;
import at.pegelhub.connector.tstp.task.TstpRuntimeTask;
import at.pegelhub.connector.tstp.task.TstpTaskFactory;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.PegelHubClientFactory;
import at.pegelhub.lib.config.ConnectorConfigDirectory;
import at.pegelhub.lib.runtime.ConnectorModule;
import at.pegelhub.lib.runtime.ConnectorRuntimeAssembly;
import at.pegelhub.lib.runtime.ConnectorRuntimeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TstpConnectorModule implements ConnectorModule {
    private static final Logger LOG = LoggerFactory.getLogger(TstpConnectorModule.class);
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
            PegelHubClient client = runtime.own(coreClients.create(config.coreConnection()));
            TstpRuntimeTask tstpTask = TstpTaskFactory.getTstpTask(config, client);
            runtime.own(tstpTask.closeable());
            LOG.info("created tstp task");

            runtime.fixedDelayTask("tstp-poll", tstpTask.task(), config.pollInterval());
            return runtime.complete();
        }
    }
}
