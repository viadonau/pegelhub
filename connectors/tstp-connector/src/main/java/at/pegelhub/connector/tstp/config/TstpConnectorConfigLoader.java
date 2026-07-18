package at.pegelhub.connector.tstp.config;

import at.pegelhub.connector.tstp.TstpMapping;
import at.pegelhub.lib.config.ConnectorConfigDirectory;
import at.pegelhub.lib.config.ConnectorMappingLoader;
import at.pegelhub.lib.config.CoreConnection;
import at.pegelhub.lib.config.LoadedMapping;
import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.config.MappingFilesConfig;
import at.pegelhub.lib.config.PollingConfig;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public final class TstpConnectorConfigLoader {
    private static final String CONNECTOR_NAME = "TSTP Connector";

    public TstpConnectorConfig load(ConnectorConfigDirectory configDirectory) throws IOException {
        TstpConfigFile configFile = configDirectory.readYaml("connector.yaml", TstpConfigFile.class);
        Duration pollInterval = configFile.polling().duration();
        LoadedMapping<TstpMapping> loadedMapping = ConnectorMappingLoader.loadExactlyOne(
                configDirectory,
                CONNECTOR_NAME,
                MappingFilesConfig.directoryOf(configFile.mappings()),
                TstpMapping.class);
        ConnectorMappingLoader.requireDirections(
                CONNECTOR_NAME,
                List.of(loadedMapping),
                MappingDirection.EXTERNAL_TO_CORE,
                MappingDirection.CORE_TO_EXTERNAL);

        return new TstpConnectorConfig(
                configFile.core(),
                configFile.tstp().server(),
                pollInterval,
                loadedMapping.value()
        );
    }

    private record TstpConfigFile(
            CoreConnection core,
            PollingConfig polling,
            MappingFilesConfig mappings,
            TstpSection tstp
    ) {
        private TstpConfigFile {
            Objects.requireNonNull(core, "core");
            Objects.requireNonNull(polling, "polling");
            Objects.requireNonNull(tstp, "tstp");
        }
    }

    private record TstpSection(
            TstpServer server
    ) {
        private TstpSection {
            Objects.requireNonNull(server, "tstp.server");
        }
    }
}
