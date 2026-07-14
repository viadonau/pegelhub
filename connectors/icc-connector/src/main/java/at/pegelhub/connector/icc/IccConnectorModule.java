package at.pegelhub.connector.icc;

import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.CoreConfig;
import at.pegelhub.lib.config.CoreEndpointConfig;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public final class IccConnectorModule implements ConnectorModule {
    private static final Logger LOG = LoggerFactory.getLogger(IccConnectorModule.class);

    @Override
    public String name() {
        return "ICC Connector";
    }

    @Override
    public ConnectorRuntimeDefinition define(ConnectorBootstrap bootstrap) throws Exception {
        IccConnectorSettings settings = getConnectorSettings(bootstrap);

        LOG.info("CoreUrl: {}", settings.coreConnection().baseUrl());
        LOG.info("ExternalCoreUrl: {}", settings.externalConnection().baseUrl());
        LOG.info("Mappings: {}", settings.mappings());
        LOG.info("Interval: {}", settings.pollInterval());

        try (ConnectorRuntimeAssembly runtime = ConnectorRuntimeAssembly.begin(name())) {
            PegelHubClient coreClient = runtime.own(bootstrap.openCoreClient(settings.coreConnection()));
            PegelHubClient externalClient = runtime.own(bootstrap.openCoreClient(settings.externalConnection()));

            runtime.fixedDelayTask(
                    "icc-sync",
                    new IccSynchronizer(coreClient, externalClient, settings.mappings(), settings.pollInterval()),
                    settings.pollInterval());
            return runtime.complete();
        }
    }

    IccConnectorSettings getConnectorSettings(ConnectorBootstrap bootstrap) throws IOException {
        IccConfigFile config = bootstrap.loadYaml("connector.yaml", IccConfigFile.class);
        List<IccMapping> mappings = loadMappings(bootstrap, config.mappingsDirectory());
        return new IccConnectorSettings(
                config.coreConnection(),
                config.externalCore().connection(),
                config.scheduleInterval(),
                mappings);
    }

    List<IccMapping> loadMappings(ConnectorBootstrap bootstrap, String mappingsDirectory) throws IOException {
        List<LoadedMapping<IccMapping>> loaded = ConnectorMappingLoader.loadRequired(
                bootstrap,
                name(),
                mappingsDirectory,
                IccMapping.class);
        ConnectorMappingLoader.requireDirections(
                name(),
                loaded,
                MappingDirection.EXTERNAL_TO_CORE,
                MappingDirection.CORE_TO_EXTERNAL);
        return loaded.stream().map(LoadedMapping::value).toList();
    }

    private record IccConfigFile(
            CoreConfig core,
            KeycloakConfig keycloak,
            CoreEndpointConfig externalCore,
            ScheduleConfig schedule,
            String mappingsDir) implements StandardConnectorConfig {
        private IccConfigFile {
            Objects.requireNonNull(core, "core");
            Objects.requireNonNull(keycloak, "keycloak");
            Objects.requireNonNull(externalCore, "externalCore");
            Objects.requireNonNull(schedule, "schedule");
        }
    }
}
