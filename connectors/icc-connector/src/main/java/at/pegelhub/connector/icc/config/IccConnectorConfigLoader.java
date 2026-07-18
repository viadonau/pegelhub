package at.pegelhub.connector.icc.config;

import at.pegelhub.connector.icc.IccMapping;
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

public final class IccConnectorConfigLoader {
    private static final String CONNECTOR_NAME = "ICC Connector";

    public IccConnectorConfig load(ConnectorConfigDirectory configDirectory) throws IOException {
        IccConfigFile configFile = configDirectory.readYaml("connector.yaml", IccConfigFile.class);
        Duration pollInterval = configFile.polling().duration();
        List<LoadedMapping<IccMapping>> loadedMappings = ConnectorMappingLoader.loadRequired(
                configDirectory,
                CONNECTOR_NAME,
                MappingFilesConfig.directoryOf(configFile.mappings()),
                IccMapping.class);
        ConnectorMappingLoader.requireDirections(
                CONNECTOR_NAME,
                loadedMappings,
                MappingDirection.EXTERNAL_TO_CORE,
                MappingDirection.CORE_TO_EXTERNAL);

        return new IccConnectorConfig(
                configFile.localCore(),
                configFile.remoteCore(),
                pollInterval,
                loadedMappings.stream().map(LoadedMapping::value).toList()
        );
    }

    private record IccConfigFile(
            CoreConnection localCore,
            CoreConnection remoteCore,
            PollingConfig polling,
            MappingFilesConfig mappings
    ) {
        private IccConfigFile {
            Objects.requireNonNull(localCore, "localCore");
            Objects.requireNonNull(remoteCore, "remoteCore");
            Objects.requireNonNull(polling, "polling");
        }
    }
}
