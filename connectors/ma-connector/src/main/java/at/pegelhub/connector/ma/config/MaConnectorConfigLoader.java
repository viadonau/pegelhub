package at.pegelhub.connector.ma.config;

import at.pegelhub.connector.ma.core.InputMapping;
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

public final class MaConnectorConfigLoader {
    private static final String CONNECTOR_NAME = "mA Connector";

    public MaConnectorConfig load(ConnectorConfigDirectory configDirectory) throws IOException {
        MaConfigFile configFile = configDirectory.readYaml("connector.yaml", MaConfigFile.class);
        Duration pollInterval = configFile.polling().duration();
        List<LoadedMapping<InputMapping>> loadedMappings = ConnectorMappingLoader.loadRequired(
                configDirectory,
                CONNECTOR_NAME,
                MappingFilesConfig.directoryOf(configFile.mappings()),
                InputMapping.class);
        ConnectorMappingLoader.requireDirections(
                CONNECTOR_NAME,
                loadedMappings,
                MappingDirection.EXTERNAL_TO_CORE);

        return new MaConnectorConfig(
                configFile.core(),
                pollInterval,
                loadedMappings.stream().map(LoadedMapping::value).toList()
        );
    }

    private record MaConfigFile(
            CoreConnection core,
            PollingConfig polling,
            MappingFilesConfig mappings
    ) {
        private MaConfigFile {
            Objects.requireNonNull(core, "core");
            Objects.requireNonNull(polling, "polling");
        }
    }
}
