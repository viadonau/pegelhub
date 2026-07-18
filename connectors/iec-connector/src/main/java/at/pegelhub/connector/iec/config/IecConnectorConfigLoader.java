package at.pegelhub.connector.iec.config;

import at.pegelhub.connector.iec.datapoints.DataPointMapping;
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

public final class IecConnectorConfigLoader {
    private static final String CONNECTOR_NAME = "IEC Connector";

    public IecConnectorConfig load(ConnectorConfigDirectory configDirectory) throws IOException {
        IecConfigFile configFile = configDirectory.readYaml("connector.yaml", IecConfigFile.class);
        Duration pollInterval = configFile.polling().duration();
        List<LoadedMapping<DataPointMapping>> loadedMappings = ConnectorMappingLoader.loadRequired(
                configDirectory,
                CONNECTOR_NAME,
                MappingFilesConfig.directoryOf(configFile.mappings()),
                DataPointMapping.class);
        ConnectorMappingLoader.requireDirections(
                CONNECTOR_NAME,
                loadedMappings,
                MappingDirection.EXTERNAL_TO_CORE,
                MappingDirection.CORE_TO_EXTERNAL);

        return new IecConnectorConfig(
                configFile.core(),
                configFile.iec().server(),
                pollInterval,
                loadedMappings.stream().map(LoadedMapping::value).toList()
        );
    }

    private record IecConfigFile(
            CoreConnection core,
            PollingConfig polling,
            MappingFilesConfig mappings,
            IecSection iec
    ) {
        private IecConfigFile {
            Objects.requireNonNull(core, "core");
            Objects.requireNonNull(polling, "polling");
            Objects.requireNonNull(iec, "iec");
        }
    }

    private record IecSection(
            IecServer server
    ) {
        private IecSection {
            Objects.requireNonNull(server, "iec.server");
        }
    }
}
