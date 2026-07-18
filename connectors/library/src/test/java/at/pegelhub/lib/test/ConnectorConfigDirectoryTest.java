package at.pegelhub.lib.test;

import at.pegelhub.lib.config.ConnectorConfigDirectory;
import at.pegelhub.lib.config.PollingConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConnectorConfigDirectoryTest {
    @TempDir
    Path directory;

    @Test
    void resolvesFilesAgainstExplicitConfigDirectory() {
        ConnectorConfigDirectory configDirectory = ConnectorConfigDirectory.at(directory);

        assertEquals(directory, configDirectory.path());
        assertEquals(directory.resolve("connector.yaml"), configDirectory.resolve("connector.yaml"));
    }

    @Test
    void readsTypedYamlRelativeToTheConfigDirectory() throws Exception {
        Files.writeString(directory.resolve("connector.yaml"), """
                interval: "42s"
                """);

        PollingConfig config = ConnectorConfigDirectory.at(directory)
                .readYaml("connector.yaml", PollingConfig.class);

        assertEquals("42s", config.interval());
    }
}
