package at.pegelhub.connector.icc;

import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.CoreConfig;
import at.pegelhub.lib.config.CoreEndpointConfig;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public final class IccConnectorModule implements ConnectorModule {
    private static final Logger LOG = LoggerFactory.getLogger(IccConnectorModule.class);

    @Override
    public String name() {
        return "ICC Connector";
    }

    @Override
    public ConnectorPlan plan(ConnectorContext context) throws Exception {
        IccConnectorOptions options = getConnectorOptions(context);

        LOG.info("CoreUrl: {}", options.coreConnection().baseUrl());
        LOG.info("ExternalCoreUrl: {}", options.externalConnection().baseUrl());
        LOG.info("Mappings: {}", options.mappings());
        LOG.info("Interval: {}", options.delay());

        try (ConnectorResources resources = ConnectorResources.create()) {
            PegelHubClient coreClient = resources.add(context.coreClient(options.coreConnection()));
            PegelHubClient externalClient = resources.add(context.coreClient(options.externalConnection()));

            ConnectorPlan.Builder builder = ConnectorPlan.builder(name())
                    .fixedDelayTask(
                            "icc-sync",
                            new IccTask(coreClient, externalClient, options.mappings(), options.lookbackWindow()),
                            options.delay());
            resources.transferTo(builder);
            return builder.build();
        }
    }

    IccConnectorOptions getConnectorOptions(ConnectorContext context) throws IOException {
        ConnectorConfig config = context.loadYaml(ConnectorConfigs.CONNECTOR_CONFIG_FILE, ConnectorConfig.class);
        List<IccMapping> mappings = loadMappings(context, ConnectorConfigs.mappingsDir(config));
        String lookbackWindow = config.schedule().delay();
        return new IccConnectorOptions(
                ConnectorConfigs.coreConnection(config),
                ConnectorConfigs.coreConnection(config.externalCore()),
                ConnectorConfigs.delay(context, config),
                lookbackWindow,
                mappings);
    }

    List<IccMapping> loadMappings(ConnectorContext context, String mappingsDir) throws IOException {
        List<IccMapping> mappings = ConnectorMappings.loadRequired(
                context,
                name(),
                mappingsDir,
                IccMapping.class);
        ConnectorMappings.requireDirections(
                name(),
                mappings,
                MappingDirection.EXTERNAL_TO_CORE,
                MappingDirection.CORE_TO_EXTERNAL);
        return mappings;
    }

    private record ConnectorConfig(
            CoreConfig core,
            KeycloakConfig keycloak,
            CoreEndpointConfig externalCore,
            ScheduleConfig schedule,
            String mappingsDir) implements StandardConnectorConfig {
        private ConnectorConfig {
            Objects.requireNonNull(core, "core");
            Objects.requireNonNull(keycloak, "keycloak");
            Objects.requireNonNull(externalCore, "externalCore");
            Objects.requireNonNull(schedule, "schedule");
        }
    }
}
