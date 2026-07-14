package at.pegelhub.lib.runtime;

import at.pegelhub.lib.CoreConnection;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.PegelHubClientFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class ConnectorBootstrap {
    public static final Path DEFAULT_CONFIG_DIRECTORY = Path.of("/app/config");

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory())
            .registerModule(new JavaTimeModule());

    private final Path configDirectory;
    private final PegelHubClientFactory clientFactory;

    private ConnectorBootstrap(Path configDirectory, PegelHubClientFactory clientFactory) {
        this.configDirectory = Objects.requireNonNull(configDirectory, "configDirectory");
        this.clientFactory = Objects.requireNonNull(clientFactory, "clientFactory");
    }

    public static ConnectorBootstrap fromArgs(String[] args) {
        return fromArgs(args, PegelHubClientFactory.http());
    }

    public static ConnectorBootstrap fromArgs(String[] args, PegelHubClientFactory clientFactory) {
        Objects.requireNonNull(args, "args");
        Path directory = args.length == 0 ? DEFAULT_CONFIG_DIRECTORY : Path.of(args[0]);
        return new ConnectorBootstrap(directory, clientFactory);
    }

    public static ConnectorBootstrap forDirectory(Path directory, PegelHubClientFactory clientFactory) {
        return new ConnectorBootstrap(directory, clientFactory);
    }

    public Path configDirectory() {
        return configDirectory;
    }

    public Path resolve(String fileName) {
        return configDirectory.resolve(fileName);
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
                    .filter(ConnectorBootstrap::isYaml)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
    }

    public PegelHubClient openCoreClient(CoreConnection connection) {
        return clientFactory.create(connection);
    }

    private static boolean isYaml(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        return name.endsWith(".yaml") || name.endsWith(".yml");
    }
}
