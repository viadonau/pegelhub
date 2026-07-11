package at.pegelhub.connector.iec.app;

import at.pegelhub.connector.iec.config.ConnectorOptions;
import at.pegelhub.connector.iec.datapoints.DataPointMapping;
import at.pegelhub.connector.iec.datapoints.DataPointRegistry;
import at.pegelhub.connector.iec.iec.IecClient;
import at.pegelhub.connector.iec.iec.impl.IecClientImpl;
import at.pegelhub.connector.iec.jobs.IecReadJob;
import at.pegelhub.connector.iec.jobs.IecWriteJob;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.CoreConfig;
import at.pegelhub.lib.config.KeycloakConfig;
import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.config.ScheduleConfig;
import at.pegelhub.lib.config.StandardConnectorConfig;
import at.pegelhub.lib.runtime.ConnectorConfigs;
import at.pegelhub.lib.runtime.ConnectorContext;
import at.pegelhub.lib.runtime.ConnectorMappings;
import at.pegelhub.lib.runtime.ConnectorModule;
import at.pegelhub.lib.runtime.ConnectorPlan;
import at.pegelhub.lib.runtime.ConnectorResources;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public final class IecConnectorModule implements ConnectorModule {
    @Override
    public String name() {
        return "IEC Connector";
    }

    @Override
    public ConnectorPlan plan(ConnectorContext context) throws Exception {
        ConnectorOptions config = getConnectorOptions(context);
        List<DataPointMapping> mappings = loadMappings(context, config.mappingsDir());

        try (ConnectorResources resources = ConnectorResources.create()) {
            PegelHubClient client = resources.add(context.coreClient(config.coreConnection()));
            DataPointRegistry dataPointRegistry = new DataPointRegistry(mappings, client);
            resources.release(client);
            resources.add(dataPointRegistry);

            IecClient iecClient = new IecClientImpl(
                    config.iecHost(),
                    config.iecPort(),
                    config.commonAddress(),
                    dataPointRegistry.protocolToCoreIoas());
            resources.closeOnStop(iecClient::disconnect);

            ConnectorPlan.Builder builder = ConnectorPlan.builder(name())
                    .threadCount(2)
                    .onStart(iecClient::connect)
                    .fixedDelayTask("iec-read", new IecReadJob(iecClient, dataPointRegistry), Duration.ofSeconds(1), config.delay())
                    .fixedDelayTask("iec-write", new IecWriteJob(iecClient, dataPointRegistry), Duration.ofSeconds(1), config.delay());
            resources.transferTo(builder);
            return builder.build();
        }
    }

    List<DataPointMapping> loadMappings(ConnectorContext context, String mappingsDir) throws IOException {
        List<DataPointMapping> mappings = ConnectorMappings.loadRequired(
                context,
                name(),
                mappingsDir,
                DataPointMapping.class);
        ConnectorMappings.requireDirections(
                name(),
                mappings,
                MappingDirection.EXTERNAL_TO_CORE,
                MappingDirection.CORE_TO_EXTERNAL);
        return mappings;
    }

    ConnectorOptions getConnectorOptions(ConnectorContext context) throws IOException {
        ConnectorConfig config = context.loadYaml(ConnectorConfigs.CONNECTOR_CONFIG_FILE, ConnectorConfig.class);

        return new ConnectorOptions(
                ConnectorConfigs.coreConnection(config),
                ConnectorConfigs.mappingsDir(config),
                InetAddress.getByName(config.iec().address()),
                config.iec().port(),
                config.iec().commonAddress(),
                ConnectorConfigs.delay(context, config)
        );
    }

    private record ConnectorConfig(
            CoreConfig core,
            KeycloakConfig keycloak,
            ScheduleConfig schedule,
            String mappingsDir,
            IecConfig iec) implements StandardConnectorConfig {
        private ConnectorConfig {
            Objects.requireNonNull(core, "core");
            Objects.requireNonNull(keycloak, "keycloak");
            Objects.requireNonNull(schedule, "schedule");
            Objects.requireNonNull(iec, "iec");
        }
    }

    private record IecConfig(String address, int port, int commonAddress) {
    }
}
