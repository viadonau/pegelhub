package at.pegelhub.connector.tstp.config;

import at.pegelhub.connector.tstp.TstpMapping;
import at.pegelhub.connector.tstp.TstpParameter;
import at.pegelhub.lib.config.ConnectorConfigDirectory;
import at.pegelhub.lib.config.ConnectorMappingLoader;
import at.pegelhub.lib.config.CoreConnection;
import at.pegelhub.lib.config.LoadedMapping;
import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.config.MappingFilesConfig;
import at.pegelhub.lib.config.WindowedPollingConfig;

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
        Duration overlap = configFile.polling().overlapDuration();

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
        validateMeanExports(loadedMappings, configFile.tstp().server(), overlap);

        return new TstpConnectorConfig(
                configFile.core(),
                configFile.tstp().server(),
                pollInterval,
                overlap,
                loadedMappings.stream().map(LoadedMapping::value).toList()
        );
    }

    private static void validateMeanExports(
            List<LoadedMapping<TstpMapping>> mappings,
            TstpServer server,
            Duration overlap) {
        String serverTimeBasis = "Z".equals(server.timeOffset()) ? "UTC" : server.timeOffset();
        for (LoadedMapping<TstpMapping> loaded : mappings) {
            var export = loaded.value().meanExport();
            if (export == null) {
                continue;
            }
            if (!serverTimeBasis.equals(export.timeBasis())) {
                throw invalid(loaded, "meanExport.timeBasis must match tstp.server.timeOffset");
            }
            // A 1,000-interval request must retain room for at least one interval beyond replay.
            if (overlap.compareTo(export.width().multipliedBy(999)) > 0) {
                throw invalid(loaded, "polling.overlap must not exceed 999 export intervals");
            }
        }
    }

    private static void validateMappings(List<LoadedMapping<TstpMapping>> mappings) {
        Set<TstpNode> outboundTargets = new HashSet<>();
        Set<UUID> inboundTargets = new HashSet<>();
        Set<MappingKey> seen = new HashSet<>();
        Map<MappingNode, Set<MappingNode>> graph = new HashMap<>();

        for (LoadedMapping<TstpMapping> loaded : mappings) {
            TstpMapping mapping = loaded.value();
            TstpNode tstpTarget = new TstpNode(mapping.stationId(), mapping.parameter());
            MappingKey key = new MappingKey(mapping.timeSeriesId(), tstpTarget, mapping.direction());

            if (!seen.add(key)) {
                throw invalid(loaded, "duplicates another mapping");
            }

            if (mapping.direction() == MappingDirection.CORE_TO_EXTERNAL) {
                if (!outboundTargets.add(tstpTarget)) {
                    throw invalid(loaded, "duplicates outbound TSTP target station " + mapping.stationId()
                            + " parameter " + mapping.parameter().value());
                }
            } else if (!inboundTargets.add(mapping.timeSeriesId())) {
                throw invalid(loaded, "duplicates inbound Core target " + mapping.timeSeriesId());
            }

            MappingNode source = mapping.direction() == MappingDirection.CORE_TO_EXTERNAL
                    ? new CoreNode(mapping.timeSeriesId())
                    : tstpTarget;
            MappingNode target = mapping.direction() == MappingDirection.CORE_TO_EXTERNAL
                    ? tstpTarget
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
            WindowedPollingConfig polling,
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
            TstpNode target,
            MappingDirection direction
    ) {}

    private sealed interface MappingNode permits CoreNode, TstpNode {}

    private record CoreNode(
            UUID timeSeriesId
    ) implements MappingNode {}

    private record TstpNode(
            int stationId,
            TstpParameter parameter
    ) implements MappingNode {}
}
