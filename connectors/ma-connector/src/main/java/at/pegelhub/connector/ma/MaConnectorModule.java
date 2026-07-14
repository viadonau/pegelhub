package at.pegelhub.connector.ma;

import at.pegelhub.connector.ma.config.MaConnectorSettings;
import at.pegelhub.connector.ma.core.InputMapping;
import at.pegelhub.connector.ma.core.MaInputMappingIndex;
import at.pegelhub.connector.ma.core.MaInputPollingJob;
import at.pegelhub.connector.ma.jni.RevPiReader;
import at.pegelhub.connector.ma.jni.RevPiReaderImpl;
import at.pegelhub.lib.PegelHubClient;
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
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public final class MaConnectorModule implements ConnectorModule {
    @Override
    public String name() {
        return "mA Connector";
    }

    @Override
    public ConnectorRuntimeDefinition define(ConnectorBootstrap bootstrap) throws Exception {
        MaConnectorSettings settings = getConnectorSettings(bootstrap);
        List<InputMapping> mappings = loadMappings(bootstrap, settings.mappingsDirectory());

        try (ConnectorRuntimeAssembly runtime = ConnectorRuntimeAssembly.begin(name())) {
            PegelHubClient client = runtime.own(bootstrap.openCoreClient(settings.coreConnection()));

            RevPiReader revPiReader = new RevPiReaderImpl();
            runtime.own(revPiReader::close);
            MaInputMappingIndex mappingIndex = new MaInputMappingIndex(revPiReader, mappings);
            MaInputPollingJob pollingJob = new MaInputPollingJob(mappingIndex, revPiReader, client);

            runtime.onStart(mappingIndex::loadInputs)
                    .fixedDelayTask("ma-input-poll", pollingJob,
                            Duration.ofSeconds(1), settings.pollInterval());
            return runtime.complete();
        }
    }

    List<InputMapping> loadMappings(ConnectorBootstrap bootstrap, String mappingsDirectory) throws IOException {
        List<LoadedMapping<InputMapping>> loaded = ConnectorMappingLoader.loadRequired(
                bootstrap,
                name(),
                mappingsDirectory,
                InputMapping.class);
        ConnectorMappingLoader.requireDirections(name(), loaded, MappingDirection.EXTERNAL_TO_CORE);
        return loaded.stream().map(LoadedMapping::value).toList();
    }

    MaConnectorSettings getConnectorSettings(ConnectorBootstrap bootstrap) throws IOException {
        MaConfigFile config = bootstrap.loadYaml("connector.yaml", MaConfigFile.class);

        return new MaConnectorSettings(
                config.coreConnection(),
                config.scheduleInterval(),
                config.mappingsDirectory()
        );
    }

    private record MaConfigFile(CoreConfig core, KeycloakConfig keycloak, ScheduleConfig schedule, String mappingsDir)
            implements StandardConnectorConfig {
        private MaConfigFile {
            Objects.requireNonNull(core, "core");
            Objects.requireNonNull(keycloak, "keycloak");
            Objects.requireNonNull(schedule, "schedule");
        }
    }
}
