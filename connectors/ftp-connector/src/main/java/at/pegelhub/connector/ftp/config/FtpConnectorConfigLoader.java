package at.pegelhub.connector.ftp.config;

import at.pegelhub.connector.ftp.FtpImportMapping;
import at.pegelhub.lib.config.ConnectorConfigDirectory;
import at.pegelhub.lib.config.ConnectorMappingLoader;
import at.pegelhub.lib.config.CoreConnection;
import at.pegelhub.lib.config.DirectedMapping;
import at.pegelhub.lib.config.LoadedMapping;
import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.config.MappingFilesConfig;
import at.pegelhub.lib.config.PollingConfig;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class FtpConnectorConfigLoader {
    private static final String CONNECTOR_NAME = "FTP Connector";

    public FtpConnectorConfig load(ConnectorConfigDirectory configDirectory) throws IOException {
        FtpConfigFile configFile = configDirectory.readYaml("connector.yaml", FtpConfigFile.class);
        Duration pollInterval = configFile.polling().duration();
        LoadedMapping<FtpMapping> loadedMapping = ConnectorMappingLoader.loadExactlyOne(
                configDirectory,
                CONNECTOR_NAME,
                MappingFilesConfig.directoryOf(configFile.mappings()),
                FtpMapping.class);
        ConnectorMappingLoader.requireDirections(
                CONNECTOR_NAME,
                List.of(loadedMapping),
                MappingDirection.EXTERNAL_TO_CORE);
        FtpMapping mapping = loadedMapping.value();

        return new FtpConnectorConfig(
                configFile.core(),
                configFile.ftp().server(),
                configFile.ftp().source(),
                pollInterval,
                new FtpImportMapping(mapping.stationId(), mapping.parameter(), mapping.timeSeriesId())
        );
    }

    private record FtpConfigFile(
            CoreConnection core,
            PollingConfig polling,
            MappingFilesConfig mappings,
            FtpSection ftp
    ) {
        private FtpConfigFile {
            Objects.requireNonNull(core, "core");
            Objects.requireNonNull(polling, "polling");
            Objects.requireNonNull(ftp, "ftp");
        }
    }

    private record FtpSection(
            FtpServer server,
            FtpSource source
    ) {
        private FtpSection {
            Objects.requireNonNull(server, "ftp.server");
            Objects.requireNonNull(source, "ftp.source");
        }
    }

    private record FtpMapping(
            UUID timeSeriesId,
            Integer stationId,
            String parameter,
            MappingDirection direction
    ) implements DirectedMapping {
        private FtpMapping {
            Objects.requireNonNull(timeSeriesId, "timeSeriesId");
            Objects.requireNonNull(stationId, "stationId");
            direction = direction == null ? MappingDirection.EXTERNAL_TO_CORE : direction;
            parameter = optional(parameter);
        }
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
