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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class TstpConnectorConfigLoader {
    private static final String CONNECTOR_NAME = "TSTP Connector";

    public TstpConnectorConfig load(ConnectorConfigDirectory configDirectory) throws IOException {
        TstpConfigFile configFile = configDirectory.readYaml("connector.yaml", TstpConfigFile.class);
        Duration pollInterval = configFile.polling().duration();

        List<LoadedMapping<TstpMapping>> loadedMappings = ConnectorMappingLoader.loadRequired(
                configDirectory,
                CONNECTOR_NAME,
                MappingFilesConfig.directoryOf(configFile.mappings()),
                TstpMapping.class);

        ConnectorMappingLoader.requireDirections(
                CONNECTOR_NAME,
                loadedMappings,
                MappingDirection.EXTERNAL_TO_CORE,
                MappingDirection.CORE_TO_EXTERNAL);

        validateMappings(loadedMappings);

        return new TstpConnectorConfig(
                configFile.core(),
                configFile.tstp().server(),
                pollInterval,
                loadedMappings.stream().map(LoadedMapping::value).toList()
        );
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
            } else if (!inboundTargets.add(mapping.timeSeriesId())) {
                throw invalid(loaded, "duplicates inbound Core target " + mapping.timeSeriesId());
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
            Set<MappingNode> visited
    ) {
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

    private record MappingKey(
            UUID timeSeriesId,
            int stationId,
            MappingDirection direction
    ) {}

    private sealed interface MappingNode permits CoreNode, TstpNode {}

    private record CoreNode(
            UUID timeSeriesId
    ) implements MappingNode {}

    private record TstpNode(
            int stationId
    ) implements MappingNode {}
}
