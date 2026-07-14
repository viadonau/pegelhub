package at.pegelhub.connector.ftp;

import at.pegelhub.connector.ftp.fileparsing.ParserFactory;
import at.pegelhub.connector.ftp.fileparsing.ParserType;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.ConfigValidation;
import at.pegelhub.lib.config.CoreConfig;
import at.pegelhub.lib.config.DirectedMapping;
import at.pegelhub.lib.config.KeycloakConfig;
import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.config.ScheduleConfig;
import at.pegelhub.lib.config.StandardConnectorConfig;
import at.pegelhub.lib.runtime.ConnectorBootstrap;
import at.pegelhub.lib.runtime.ConnectorMappingLoader;
import at.pegelhub.lib.runtime.LoadedMapping;
import at.pegelhub.lib.runtime.ConnectorModule;
import at.pegelhub.lib.runtime.ConnectorRuntimeAssembly;
import at.pegelhub.lib.runtime.ConnectorRuntimeDefinition;
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
    public ConnectorRuntimeDefinition define(ConnectorBootstrap bootstrap) throws Exception {
        FtpConnectorSettings settings = getConnectorSettings(bootstrap);
        try (ConnectorRuntimeAssembly runtime = ConnectorRuntimeAssembly.begin(name())) {
            FTPClient ftp = new FTPClient();
            runtime.own(ftp::disconnect);
            ftp.setControlKeepAliveTimeout(Duration.ofMinutes(15));
            ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(new LogOutputStream(LOG))));
            ftp.setDataTimeout(Duration.ofMinutes(15));

            PegelHubClient client = runtime.own(bootstrap.openCoreClient(settings.coreConnection()));

            runtime.fixedDelayTask("ftp-poll", new FtpImportJob(
                            ftp,
                            settings,
                            client,
                            ParserFactory.getParser(settings.parserType())), settings.pollInterval());
            return runtime.complete();
        }
    }

    FtpConnectorSettings getConnectorSettings(ConnectorBootstrap bootstrap) throws IOException {
        FtpConfigFile config = bootstrap.loadYaml("connector.yaml", FtpConfigFile.class);
        LoadedMapping<FtpMapping> loadedMapping = ConnectorMappingLoader.loadExactlyOne(
                bootstrap,
                name(),
                config.mappingsDirectory(),
                FtpMapping.class);
        ConnectorMappingLoader.requireDirections(
                name(), List.of(loadedMapping), MappingDirection.EXTERNAL_TO_CORE);
        FtpMapping mapping = loadedMapping.value();

        ParserType parserType = requireParserType(config.ftp().parserType());

        return new FtpConnectorSettings(
                config.coreConnection(),
                java.net.InetAddress.getByName(config.ftp().address()),
                config.ftp().port(),
                config.ftp().user(),
                config.ftp().password(),
                config.ftp().path(),
                parserType,
                config.scheduleInterval(),
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

    private record FtpConfigFile(
            CoreConfig core,
            KeycloakConfig keycloak,
            ScheduleConfig schedule,
            String mappingsDir,
            FtpEndpointConfig ftp) implements StandardConnectorConfig {
        private FtpConfigFile {
            Objects.requireNonNull(core, "core");
            Objects.requireNonNull(keycloak, "keycloak");
            Objects.requireNonNull(schedule, "schedule");
            Objects.requireNonNull(ftp, "ftp");
        }
    }

    private record FtpEndpointConfig(
            String address, int port, String user, String password, String path, String parserType) {
        private FtpEndpointConfig {
            address = ConfigValidation.requireText(address, "ftp.address");
            port = ConfigValidation.requireTcpPort(port, "ftp.port");
            user = ConfigValidation.requireText(user, "ftp.user");
            password = ConfigValidation.requireText(password, "ftp.password");
            path = ConfigValidation.requireText(path, "ftp.path");
            parserType = ConfigValidation.requireText(parserType, "ftp.parserType");
        }
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
