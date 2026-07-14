package at.pegelhub.connector.iec.app;

import at.pegelhub.connector.iec.config.IecConnectorSettings;
import at.pegelhub.connector.iec.datapoints.DataPointMapping;
import at.pegelhub.connector.iec.datapoints.IecMappingIndex;
import at.pegelhub.connector.iec.iec.IecClient;
import at.pegelhub.connector.iec.iec.impl.IecClientImpl;
import at.pegelhub.connector.iec.jobs.IecToCoreJob;
import at.pegelhub.connector.iec.jobs.CoreToIecJob;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.ConfigValidation;
import at.pegelhub.lib.config.CoreConfig;
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

import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public final class IecConnectorModule implements ConnectorModule {
    @Override
    public String name() {
        return "IEC Connector";
    }

    @Override
    public ConnectorRuntimeDefinition define(ConnectorBootstrap bootstrap) throws Exception {
        IecConnectorSettings settings = getConnectorSettings(bootstrap);
        List<DataPointMapping> mappings = loadMappings(bootstrap, settings.mappingsDirectory());
        IecMappingIndex mappingIndex = new IecMappingIndex(mappings);

        try (ConnectorRuntimeAssembly runtime = ConnectorRuntimeAssembly.begin(name())) {
            PegelHubClient client = runtime.own(bootstrap.openCoreClient(settings.coreConnection()));

            IecClient iecClient = new IecClientImpl(
                    settings.iecHost(),
                    settings.iecPort(),
                    settings.commonAddress(),
                    mappingIndex.protocolToCoreIoas());
            runtime.own(iecClient::disconnect);

            runtime
                    .threadCount(2)
                    .onStart(iecClient::connect)
                    .fixedDelayTask("iec-to-core", new IecToCoreJob(iecClient, mappingIndex, client),
                            Duration.ofSeconds(1), settings.pollInterval())
                    .fixedDelayTask("core-to-iec", new CoreToIecJob(iecClient, mappingIndex, client),
                            Duration.ofSeconds(1), settings.pollInterval());
            return runtime.complete();
        }
    }

    List<DataPointMapping> loadMappings(ConnectorBootstrap bootstrap, String mappingsDirectory) throws IOException {
        List<LoadedMapping<DataPointMapping>> loaded = ConnectorMappingLoader.loadRequired(
                bootstrap,
                name(),
                mappingsDirectory,
                DataPointMapping.class);
        ConnectorMappingLoader.requireDirections(
                name(),
                loaded,
                MappingDirection.EXTERNAL_TO_CORE,
                MappingDirection.CORE_TO_EXTERNAL);
        return loaded.stream().map(LoadedMapping::value).toList();
    }

    IecConnectorSettings getConnectorSettings(ConnectorBootstrap bootstrap) throws IOException {
        IecConfigFile config = bootstrap.loadYaml("connector.yaml", IecConfigFile.class);

        return new IecConnectorSettings(
                config.coreConnection(),
                config.mappingsDirectory(),
                InetAddress.getByName(config.iec().address()),
                config.iec().port(),
                config.iec().commonAddress(),
                config.scheduleInterval()
        );
    }

    private record IecConfigFile(
            CoreConfig core,
            KeycloakConfig keycloak,
            ScheduleConfig schedule,
            String mappingsDir,
            IecEndpointConfig iec) implements StandardConnectorConfig {
        private IecConfigFile {
            Objects.requireNonNull(core, "core");
            Objects.requireNonNull(keycloak, "keycloak");
            Objects.requireNonNull(schedule, "schedule");
            Objects.requireNonNull(iec, "iec");
        }
    }

    private record IecEndpointConfig(String address, int port, int commonAddress) {
        private IecEndpointConfig {
            address = ConfigValidation.requireText(address, "iec.address");
            port = ConfigValidation.requireTcpPort(port, "iec.port");
            commonAddress = ConfigValidation.requirePositive(commonAddress, "iec.commonAddress");
        }
    }
}
