package at.pegelhub.lib.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public final class ConnectorMappingLoader {
    private ConnectorMappingLoader() {
    }

    public static <T> List<LoadedMapping<T>> loadRequired(
            ConnectorConfigDirectory configDirectory,
            String connectorName,
            String mappingsDirectory,
            Class<T> type) throws IOException {
        List<LoadedMapping<T>> mappings = loadAll(configDirectory, mappingsDirectory, type);
        if (mappings.isEmpty()) {
            throw new IllegalArgumentException(
                    connectorName + " requires at least one mapping file in " + mappingsDirectory);
        }
        return mappings;
    }

    public static <T> LoadedMapping<T> loadExactlyOne(
            ConnectorConfigDirectory configDirectory,
            String connectorName,
            String mappingsDirectory,
            Class<T> type) throws IOException {
        List<LoadedMapping<T>> mappings = loadAll(configDirectory, mappingsDirectory, type);
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
            ConnectorConfigDirectory configDirectory,
            String mappingsDirectory,
            Class<T> type) throws IOException {
        return configDirectory.listYamlFiles(mappingsDirectory).stream()
                .map(path -> loadMapping(configDirectory, path, type))
                .toList();
    }

    private static <T> LoadedMapping<T> loadMapping(
            ConnectorConfigDirectory configDirectory,
            Path path,
            Class<T> type) {
        try {
            return new LoadedMapping<>(path.getFileName().toString(), configDirectory.readYaml(path, type));
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid mapping " + path.getFileName() + ": " + e.getMessage(), e);
        }
    }
}
