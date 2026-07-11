package at.pegelhub.lib.runtime;

import at.pegelhub.lib.config.DirectedMapping;
import at.pegelhub.lib.config.MappingDirection;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public final class ConnectorMappings {
    private ConnectorMappings() {
    }

    public static <T> List<T> loadRequired(
            ConnectorContext context,
            String connectorName,
            String mappingsDir,
            Class<T> type) throws IOException {
        List<T> mappings = loadAll(context, mappingsDir, type);
        if (mappings.isEmpty()) {
            throw new IllegalArgumentException(connectorName + " requires at least one mapping file in " + mappingsDir);
        }
        return mappings;
    }

    public static <T> T loadExactlyOne(
            ConnectorContext context,
            String connectorName,
            String mappingsDir,
            Class<T> type) throws IOException {
        List<T> mappings = loadAll(context, mappingsDir, type);
        if (mappings.size() != 1) {
            throw new IllegalArgumentException(connectorName + " requires exactly one mapping file in " + mappingsDir);
        }
        return mappings.getFirst();
    }

    public static void requireDirections(
            String connectorName,
            List<? extends DirectedMapping> mappings,
            MappingDirection... allowedDirections) {
        EnumSet<MappingDirection> allowed = EnumSet.copyOf(Arrays.asList(allowedDirections));
        for (DirectedMapping mapping : mappings) {
            if (!allowed.contains(mapping.direction())) {
                throw new IllegalArgumentException(connectorName + " does not support direction: " + mapping.direction().value());
            }
        }
    }

    private static <T> List<T> loadAll(ConnectorContext context, String mappingsDir, Class<T> type) throws IOException {
        return context.listYamlFiles(mappingsDir).stream()
                .map(path -> loadMapping(context, path, type))
                .toList();
    }

    private static <T> T loadMapping(ConnectorContext context, Path path, Class<T> type) {
        try {
            return context.loadYaml(path, type);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid mapping " + path.getFileName() + ": " + e.getMessage(), e);
        }
    }
}
