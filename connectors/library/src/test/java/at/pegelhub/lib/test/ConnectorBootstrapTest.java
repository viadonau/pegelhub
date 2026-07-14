package at.pegelhub.lib.runtime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConnectorBootstrapTest {
    @TempDir
    Path directory;

    @Test
    void resolvesDefaultConfigDirWhenArgsAreEmpty() {
        ConnectorBootstrap config = ConnectorBootstrap.fromArgs(new String[0]);

        assertEquals(ConnectorBootstrap.DEFAULT_CONFIG_DIRECTORY, config.configDirectory());
    }

    @Test
    void resolvesExplicitConfigDirectory() {
        ConnectorBootstrap config = ConnectorBootstrap.fromArgs(new String[]{directory.toString()});
        assertEquals(directory, config.configDirectory());
        assertEquals(directory.resolve("connector.yaml"), config.resolve("connector.yaml"));
    }
}
