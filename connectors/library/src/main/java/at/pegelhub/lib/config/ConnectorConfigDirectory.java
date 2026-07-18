package at.pegelhub.lib.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class ConnectorConfigDirectory {
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private final Path path;

    private ConnectorConfigDirectory(Path path) {
        this.path = Objects.requireNonNull(path, "path");
    }

    public static ConnectorConfigDirectory at(Path path) {
        return new ConnectorConfigDirectory(path);
    }

    public Path path() {
        return path;
    }

    public Path resolve(String relativePath) {
        return path.resolve(relativePath);
    }

    public <T> T readYaml(String relativePath, Class<T> type) throws IOException {
        return readYaml(resolve(relativePath), type);
    }

    public <T> T readYaml(Path file, Class<T> type) throws IOException {
        return YAML.readValue(file.toFile(), type);
    }

    public List<Path> listYamlFiles(String relativeDirectory) throws IOException {
        Path directory = resolve(relativeDirectory);
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Configuration directory is not a directory: "
                    + directory.toAbsolutePath());
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(Files::isRegularFile)
                    .filter(ConnectorConfigDirectory::isYaml)
                    .sorted(Comparator.comparing(file -> file.getFileName().toString()))
                    .toList();
        }
    }

    private static boolean isYaml(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        return name.endsWith(".yaml") || name.endsWith(".yml");
    }
}
