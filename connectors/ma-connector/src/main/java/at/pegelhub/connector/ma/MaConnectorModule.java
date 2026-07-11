package at.pegelhub.connector.ma;

import at.pegelhub.connector.ma.config.MaConnectorOptions;
import at.pegelhub.connector.ma.core.InputMapping;
import at.pegelhub.connector.ma.core.InputRegistry;
import at.pegelhub.connector.ma.core.MaReadJob;
import at.pegelhub.connector.ma.jni.RevPiReader;
import at.pegelhub.connector.ma.jni.RevPiReaderImpl;
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
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public final class MaConnectorModule implements ConnectorModule {
    @Override
    public String name() {
        return "mA Connector";
    }

    @Override
    public ConnectorPlan plan(ConnectorContext context) throws Exception {
        MaConnectorOptions config = getConnectorOptions(context);
        List<InputMapping> mappings = loadMappings(context, config.mappingsDir());

        try (ConnectorResources resources = ConnectorResources.create()) {
            PegelHubClient client = resources.add(context.coreClient(config.coreConnection()));

            RevPiReader revPiReader = new RevPiReaderImpl();
            resources.closeOnStop(revPiReader::close);
            InputRegistry inputRegistry = new InputRegistry(revPiReader, mappings, client);
            resources.release(client);
            resources.add(inputRegistry);
            MaReadJob readJob = new MaReadJob(inputRegistry, revPiReader);

            ConnectorPlan.Builder builder = ConnectorPlan.builder(name())
                    .onStart(inputRegistry::loadInputs)
                    .fixedDelayTask("ma-read", readJob, Duration.ofSeconds(1), config.delay());
            resources.transferTo(builder);
            return builder.build();
        }
    }

    List<InputMapping> loadMappings(ConnectorContext context, String mappingsDir) throws IOException {
        List<InputMapping> mappings = ConnectorMappings.loadRequired(
                context,
                name(),
                mappingsDir,
                InputMapping.class);
        ConnectorMappings.requireDirections(name(), mappings, MappingDirection.EXTERNAL_TO_CORE);
        return mappings;
    }

    MaConnectorOptions getConnectorOptions(ConnectorContext context) throws IOException {
        ConnectorConfig config = context.loadYaml(ConnectorConfigs.CONNECTOR_CONFIG_FILE, ConnectorConfig.class);

        return new MaConnectorOptions(
                ConnectorConfigs.coreConnection(config),
                ConnectorConfigs.delay(context, config),
                ConnectorConfigs.mappingsDir(config)
        );
    }

    private record ConnectorConfig(CoreConfig core, KeycloakConfig keycloak, ScheduleConfig schedule, String mappingsDir)
            implements StandardConnectorConfig {
        private ConnectorConfig {
            Objects.requireNonNull(core, "core");
            Objects.requireNonNull(keycloak, "keycloak");
            Objects.requireNonNull(schedule, "schedule");
        }
    }
}
