package at.pegelhub.lib.runtime;

import at.pegelhub.lib.config.DirectedMapping;
import at.pegelhub.lib.config.MappingDirection;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public final class ConnectorMappingLoader {
    private ConnectorMappingLoader() {
    }

    public static <T> List<LoadedMapping<T>> loadRequired(
            ConnectorBootstrap bootstrap,
            String connectorName,
            String mappingsDirectory,
            Class<T> type) throws IOException {
        List<LoadedMapping<T>> mappings = loadAll(bootstrap, mappingsDirectory, type);
        if (mappings.isEmpty()) {
            throw new IllegalArgumentException(
                    connectorName + " requires at least one mapping file in " + mappingsDirectory);
        }
        return mappings;
    }

    public static <T> LoadedMapping<T> loadExactlyOne(
            ConnectorBootstrap bootstrap,
            String connectorName,
            String mappingsDirectory,
            Class<T> type) throws IOException {
        List<LoadedMapping<T>> mappings = loadAll(bootstrap, mappingsDirectory, type);
        if (mappings.size() != 1) {
            throw new IllegalArgumentException(
                    connectorName + " requires exactly one mapping file in " + mappingsDirectory);
        }
        return mappings.getFirst();
    }

    public static void requireDirections(
            String connectorName,
            List<? extends LoadedMapping<? extends DirectedMapping>> mappings,
            MappingDirection... allowedDirections) {
        EnumSet<MappingDirection> allowed = EnumSet.copyOf(Arrays.asList(allowedDirections));
        for (LoadedMapping<? extends DirectedMapping> loaded : mappings) {
            if (!allowed.contains(loaded.value().direction())) {
                throw new IllegalArgumentException(
                        connectorName + " mapping " + loaded.fileName() + " does not support direction: "
                                + loaded.value().direction().value());
            }
        }
    }

    private static <T> List<LoadedMapping<T>> loadAll(
            ConnectorBootstrap bootstrap,
            String mappingsDirectory,
            Class<T> type) throws IOException {
        return bootstrap.listYamlFiles(mappingsDirectory).stream()
                .map(path -> loadMapping(bootstrap, path, type))
                .toList();
    }

    private static <T> LoadedMapping<T> loadMapping(ConnectorBootstrap bootstrap, Path path, Class<T> type) {
        try {
            return new LoadedMapping<>(path.getFileName().toString(), bootstrap.loadYaml(path, type));
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid mapping " + path.getFileName() + ": " + e.getMessage(), e);
        }
    }
}
