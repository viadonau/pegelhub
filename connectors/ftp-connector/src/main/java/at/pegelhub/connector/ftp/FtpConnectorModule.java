package at.pegelhub.connector.ftp;

import at.pegelhub.connector.ftp.fileparsing.ParserFactory;
import at.pegelhub.connector.ftp.fileparsing.ParserType;
import at.pegelhub.lib.PegelHubClient;
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
import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class FtpConnectorModule implements ConnectorModule {
    private static final Logger LOG = LoggerFactory.getLogger(FtpConnectorModule.class);

    @Override
    public String name() {
        return "FTP Connector";
    }

    @Override
    public ConnectorPlan plan(ConnectorContext context) throws Exception {
        ConnectorOptions conOpt = getConnectorOptions(context);
        try (ConnectorResources resources = ConnectorResources.create()) {
            FTPClient ftp = new FTPClient();
            resources.closeOnStop(ftp::disconnect);
            ftp.setControlKeepAliveTimeout(Duration.ofMinutes(15));
            ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(new LogOutputStream(LOG))));
            ftp.setDataTimeout(Duration.ofMinutes(15));

            PegelHubClient client = resources.add(context.coreClient(conOpt.coreConnection()));

            ConnectorPlan.Builder builder = ConnectorPlan.builder(name())
                    .fixedDelayTask("ftp-poll", new FtpTask(
                            ftp,
                            conOpt,
                            client,
                            ParserFactory.getParser(conOpt.parserType())), conOpt.readDelay());
            resources.transferTo(builder);
            return builder.build();
        }
    }

    ConnectorOptions getConnectorOptions(ConnectorContext context) throws IOException {
        ConnectorConfig config = context.loadYaml(ConnectorConfigs.CONNECTOR_CONFIG_FILE, ConnectorConfig.class);
        FtpMapping mapping = ConnectorMappings.loadExactlyOne(
                context,
                name(),
                ConnectorConfigs.mappingsDir(config),
                FtpMapping.class);
        ConnectorMappings.requireDirections(name(), List.of(mapping), MappingDirection.EXTERNAL_TO_CORE);

        ParserType parserType = requireParserType(config.ftp().parserType());

        return new ConnectorOptions(
                ConnectorConfigs.coreConnection(config),
                java.net.InetAddress.getByName(config.ftp().address()),
                config.ftp().port(),
                config.ftp().user(),
                config.ftp().password(),
                config.ftp().path(),
                parserType,
                ConnectorConfigs.delay(context, config),
                mapping.timeSeriesId(),
                mapping.stationId(),
                mapping.parameter()
        );
    }

    private static ParserType requireParserType(String configured) {
        ParserType parserType = ParserType.valueOfName(configured);
        if (parserType == null) {
            throw new IllegalArgumentException("Unknown FTP parser type: " + configured);
        }
        return parserType;
    }

    private record ConnectorConfig(
            CoreConfig core,
            KeycloakConfig keycloak,
            ScheduleConfig schedule,
            String mappingsDir,
            FtpConfig ftp) implements StandardConnectorConfig {
        private ConnectorConfig {
            Objects.requireNonNull(core, "core");
            Objects.requireNonNull(keycloak, "keycloak");
            Objects.requireNonNull(schedule, "schedule");
            Objects.requireNonNull(ftp, "ftp");
        }
    }

    private record FtpConfig(String address, int port, String user, String password, String path, String parserType) {
    }

    private record FtpMapping(UUID timeSeriesId, Integer stationId, String parameter, MappingDirection direction)
            implements DirectedMapping {
        private FtpMapping {
            Objects.requireNonNull(timeSeriesId, "timeSeriesId");
            Objects.requireNonNull(stationId, "stationId");
            direction = direction == null ? MappingDirection.EXTERNAL_TO_CORE : direction;
            parameter = optional(parameter);
        }
    }

    private static String optional(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
