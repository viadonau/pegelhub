package at.pegelhub.connector.tstp;

import at.pegelhub.connector.tstp.task.TstpRuntimeTask;
import at.pegelhub.connector.tstp.task.TstpTaskFactory;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.ConfigValidation;
import at.pegelhub.lib.config.CoreConfig;
import at.pegelhub.lib.config.DirectedMapping;
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
import java.util.UUID;

public final class TstpConnectorModule implements ConnectorModule {
    private static final Logger LOG = LoggerFactory.getLogger(TstpConnectorModule.class);

    @Override
    public String name() {
        return "TSTP Connector";
    }

    @Override
    public ConnectorPlan plan(ConnectorContext context) throws Exception {
        ConnectorOptions conOpt = getConnectorOptions(context);
        try (ConnectorResources resources = ConnectorResources.create()) {
            PegelHubClient client = resources.add(context.coreClient(conOpt.coreConnection()));
            TstpRuntimeTask tstpTask = TstpTaskFactory.getTstpTask(conOpt, client);
            resources.release(client);
            resources.add(tstpTask.closeable());
            LOG.info("created tstp task");

            ConnectorPlan.Builder builder = ConnectorPlan.builder(name())
                    .fixedDelayTask("tstp-poll", tstpTask.task(), conOpt.readDelay());
            resources.transferTo(builder);
            return builder.build();
        }
    }

    ConnectorOptions getConnectorOptions(ConnectorContext context) throws IOException {
        ConnectorConfig config = context.loadYaml(ConnectorConfigs.CONNECTOR_CONFIG_FILE, ConnectorConfig.class);
        TstpMapping mapping = ConnectorMappings.loadExactlyOne(
                context,
                name(),
                ConnectorConfigs.mappingsDir(config),
                TstpMapping.class);
        ConnectorMappings.requireDirections(
                name(),
                List.of(mapping),
                MappingDirection.EXTERNAL_TO_CORE,
                MappingDirection.CORE_TO_EXTERNAL);

        return new ConnectorOptions(
                ConnectorConfigs.coreConnection(config),
                config.tstp().address(),
                config.tstp().port(),
                ConnectorConfigs.delay(context, config),
                mapping.timeSeriesId(),
                mapping.stationId(),
                mapping.direction());
    }

    private record ConnectorConfig(
            CoreConfig core,
            KeycloakConfig keycloak,
            ScheduleConfig schedule,
            String mappingsDir,
            TstpConfig tstp) implements StandardConnectorConfig {
        private ConnectorConfig {
            Objects.requireNonNull(core, "core");
            Objects.requireNonNull(keycloak, "keycloak");
            Objects.requireNonNull(schedule, "schedule");
            Objects.requireNonNull(tstp, "tstp");
        }
    }

    private record TstpConfig(String address, int port) {
        private TstpConfig {
            address = ConfigValidation.requireText(address, "tstp.address");
            port = ConfigValidation.requireTcpPort(port, "tstp.port");
        }
    }

    private record TstpMapping(UUID timeSeriesId, Integer stationId, MappingDirection direction)
            implements DirectedMapping {
        private TstpMapping {
            Objects.requireNonNull(timeSeriesId, "timeSeriesId");
            Objects.requireNonNull(stationId, "stationId");
            Objects.requireNonNull(direction, "direction");
        }
    }
}
