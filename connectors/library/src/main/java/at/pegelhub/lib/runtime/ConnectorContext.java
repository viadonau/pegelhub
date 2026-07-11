package at.pegelhub.lib.runtime;

import at.pegelhub.lib.ClientCredentials;
import at.pegelhub.lib.CoreConnection;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.PegelHubClientFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class ConnectorContext {
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory())
            .registerModule(new JavaTimeModule());

    private final ConnectorRuntimeConfig runtimeConfig;

    ConnectorContext(ConnectorRuntimeConfig runtimeConfig) {
        this.runtimeConfig = Objects.requireNonNull(runtimeConfig, "runtimeConfig");
    }

    public static ConnectorContext fromArgs(String[] args) {
        return new ConnectorContext(ConnectorRuntimeConfig.fromArgs(args));
    }

    public Path configDir() {
        return runtimeConfig.configDir();
    }

    public Path resolve(String fileName) {
        return runtimeConfig.resolve(fileName);
    }

    public <T> T loadYaml(String fileName, Class<T> type) throws IOException {
        return loadYaml(resolve(fileName), type);
    }

    public <T> T loadYaml(Path file, Class<T> type) throws IOException {
        return YAML.readValue(file.toFile(), type);
    }

    public List<Path> listYamlFiles(String directoryName) throws IOException {
        Path directory = resolve(directoryName);
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Mapping directory is not a directory: " + directory.toAbsolutePath());
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(Files::isRegularFile)
                    .filter(ConnectorContext::isYaml)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
    }

    public Duration parseDuration(String value) {
        return ConnectorRuntimeConfig.parseDuration(value);
    }

    public PegelHubClient coreClient(URL baseUrl, ClientCredentials credentials) {
        return PegelHubClientFactory.create(baseUrl, credentials);
    }

    public PegelHubClient coreClient(CoreConnection connection) {
        return PegelHubClientFactory.create(connection);
    }

    private static boolean isYaml(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        return name.endsWith(".yaml") || name.endsWith(".yml");
    }
}
