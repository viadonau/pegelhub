package at.pegelhub.connector.tstp;

import at.pegelhub.connector.tstp.catalog.TstpCatalogResolver;
import at.pegelhub.connector.tstp.client.HttpTstpClient;
import at.pegelhub.connector.tstp.client.TstpClient;
import at.pegelhub.connector.tstp.codec.TstpBinaryCodec;
import at.pegelhub.connector.tstp.codec.TstpXmlCodec;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.ConfigValidation;
import at.pegelhub.lib.config.CoreConfig;
import at.pegelhub.lib.config.KeycloakConfig;
import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.config.ScheduleConfig;
import at.pegelhub.lib.config.StandardConnectorConfig;
import at.pegelhub.lib.runtime.ConnectorBootstrap;
import at.pegelhub.lib.runtime.ConnectorMappingLoader;
import at.pegelhub.lib.runtime.ConnectorModule;
import at.pegelhub.lib.runtime.ConnectorRuntimeAssembly;
import at.pegelhub.lib.runtime.ConnectorRuntimeDefinition;
import at.pegelhub.lib.runtime.LoadedMapping;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class TstpConnectorModule implements ConnectorModule {
    @Override
    public String name() {
        return "TSTP Connector";
    }

    @Override
    public ConnectorRuntimeDefinition define(ConnectorBootstrap bootstrap) throws Exception {
        TstpConnectorSettings settings = getConnectorSettings(bootstrap);
        try (ConnectorRuntimeAssembly runtime = ConnectorRuntimeAssembly.begin(name())) {
            PegelHubClient coreClient = runtime.own(bootstrap.openCoreClient(settings.coreConnection()));
            TstpClient tstpClient = runtime.own(HttpTstpClient.open(
                    settings.address(),
                    settings.port(),
                    new TstpXmlCodec(new TstpBinaryCodec())));
            TstpSynchronizer synchronizer = new TstpSynchronizer(
                    coreClient,
                    tstpClient,
                    new TstpCatalogResolver(tstpClient),
                    settings.mappings(),
                    settings.pollInterval());
            runtime.fixedDelayTask("tstp-sync", synchronizer, settings.pollInterval());
            return runtime.complete();
        }
    }

    TstpConnectorSettings getConnectorSettings(ConnectorBootstrap bootstrap) throws IOException {
        TstpConfigFile config = bootstrap.loadYaml("connector.yaml", TstpConfigFile.class);
        List<LoadedMapping<TstpMapping>> mappings = ConnectorMappingLoader.loadRequired(
                bootstrap,
                name(),
                config.mappingsDirectory(),
                TstpMapping.class);
        ConnectorMappingLoader.requireDirections(
                name(),
                mappings,
                MappingDirection.EXTERNAL_TO_CORE,
                MappingDirection.CORE_TO_EXTERNAL);
        validateMappings(mappings);
        return new TstpConnectorSettings(
                config.coreConnection(),
                config.tstp().address(),
                config.tstp().port(),
                config.scheduleInterval(),
                mappings);
    }

    private static void validateMappings(List<LoadedMapping<TstpMapping>> mappings) {
        Set<Integer> outboundTargets = new HashSet<>();
        Set<UUID> inboundTargets = new HashSet<>();
        Set<MappingKey> seen = new HashSet<>();
        Map<MappingNode, Set<MappingNode>> graph = new HashMap<>();

        for (LoadedMapping<TstpMapping> loaded : mappings) {
            TstpMapping mapping = loaded.value();
            MappingKey key = new MappingKey(mapping.timeSeriesId(), mapping.stationId(), mapping.direction());
            if (!seen.add(key)) {
                throw invalid(loaded, "duplicates another mapping");
            }
            if (mapping.direction() == MappingDirection.CORE_TO_EXTERNAL) {
                if (!outboundTargets.add(mapping.stationId())) {
                    throw invalid(loaded, "duplicates outbound TSTP target station " + mapping.stationId());
                }
            } else {
                if (!inboundTargets.add(mapping.timeSeriesId())) {
                    throw invalid(loaded, "duplicates inbound Core target " + mapping.timeSeriesId());
                }
            }

            MappingNode source = mapping.direction() == MappingDirection.CORE_TO_EXTERNAL
                    ? new CoreNode(mapping.timeSeriesId())
                    : new TstpNode(mapping.stationId());
            MappingNode target = mapping.direction() == MappingDirection.CORE_TO_EXTERNAL
                    ? new TstpNode(mapping.stationId())
                    : new CoreNode(mapping.timeSeriesId());
            if (hasPath(graph, target, source, new HashSet<>())) {
                throw invalid(loaded, "creates a feedback cycle across Core series and TSTP stations");
            }
            graph.computeIfAbsent(source, ignored -> new HashSet<>()).add(target);
        }
    }

    private static boolean hasPath(
            Map<MappingNode, Set<MappingNode>> graph,
            MappingNode current,
            MappingNode target,
            Set<MappingNode> visited) {
        if (current.equals(target)) {
            return true;
        }
        if (!visited.add(current)) {
            return false;
        }
        return graph.getOrDefault(current, Set.of()).stream()
                .anyMatch(next -> hasPath(graph, next, target, visited));
    }

    private static IllegalArgumentException invalid(LoadedMapping<TstpMapping> mapping, String message) {
        return new IllegalArgumentException("Invalid TSTP mapping " + mapping.fileName() + ": " + message);
    }

    private record TstpConfigFile(
            CoreConfig core,
            KeycloakConfig keycloak,
            ScheduleConfig schedule,
            String mappingsDir,
            TstpEndpointConfig tstp) implements StandardConnectorConfig {
        private TstpConfigFile {
            Objects.requireNonNull(core, "core");
            Objects.requireNonNull(keycloak, "keycloak");
            Objects.requireNonNull(schedule, "schedule");
            Objects.requireNonNull(tstp, "tstp");
        }
    }

    private record TstpEndpointConfig(String address, int port) {
        private TstpEndpointConfig {
            address = ConfigValidation.requireText(address, "tstp.address");
            port = ConfigValidation.requireTcpPort(port, "tstp.port");
        }
    }

    private record MappingKey(UUID timeSeriesId, int stationId, MappingDirection direction) {
    }

    private sealed interface MappingNode permits CoreNode, TstpNode {
    }

    private record CoreNode(UUID timeSeriesId) implements MappingNode {
    }

    private record TstpNode(int stationId) implements MappingNode {
    }
}
