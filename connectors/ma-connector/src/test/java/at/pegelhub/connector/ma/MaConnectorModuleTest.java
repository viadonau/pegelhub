package at.pegelhub.connector.ma;

import at.pegelhub.connector.ma.config.MaConnectorConfig;
import at.pegelhub.connector.ma.config.MaConnectorConfigLoader;
import at.pegelhub.connector.ma.core.InputMapping;
import at.pegelhub.lib.config.ConnectorConfigDirectory;
import at.pegelhub.lib.config.MappingDirection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MaConnectorModuleTest {
    @TempDir
    Path tmp;

    @Test
    void shouldLoadCheckedInExampleConfiguration() throws Exception {
        MaConnectorConfig config = new MaConnectorConfigLoader().load(
                ConnectorConfigDirectory.at(Path.of("examples/config")));

        assertEquals(Duration.ofSeconds(30), config.pollInterval());
        assertEquals(2, config.mappings().size());
    }

    @Test
    void validationModeChecksConfigWithoutOpeningCoreOrRevPiConnections() throws Exception {
        new MaConnectorModule().validate(ConnectorConfigDirectory.at(Path.of("examples/config")));
    }

    @Test
    void shouldLoadAllConfigFieldsFromConnectorYaml() throws Exception {
        writeConnectorYaml("5s", "mappings");
        writeDefaultMapping();

        MaConnectorConfig config = loadConfig();

        assertEquals("http://127.0.0.1:8080/", config.coreConnection().baseUrl().toString());
        assertEquals("ma-client", config.coreConnection().authentication().clientId());
        assertEquals(Duration.ofSeconds(5), config.pollInterval());
        assertEquals(1, config.mappings().size());
    }

    @Test
    void shouldParseDelayUnitsCaseInsensitively() throws Exception {
        writeConnectorYaml("2M", null);
        writeDefaultMapping();
        MaConnectorConfig minuteConfig = loadConfig();
        assertEquals(Duration.ofMinutes(2), minuteConfig.pollInterval());

        writeConnectorYaml("1H", null);
        writeDefaultMapping();
        MaConnectorConfig hourConfig = loadConfig();
        assertEquals(Duration.ofHours(1), hourConfig.pollInterval());
    }

    @Test
    void shouldFailOnUnknownDelayUnit() throws Exception {
        writeConnectorYaml("10x", null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, this::loadConfig);
        assertEquals("polling.interval must end with s, m, or h", ex.getMessage());
    }

    @Test
    void shouldDefaultMappingsDirFromSharedConfigHelper() throws Exception {
        writeConnectorYaml("1s", null);
        writeDefaultMapping();

        MaConnectorConfig config = loadConfig();

        assertEquals(1, config.mappings().size());
    }

    @Test
    void shouldLoadMappingsInSortedOrder() throws Exception {
        writeConnectorYaml("5s", "mappings");
        Files.createDirectories(tmp.resolve("mappings"));
        writeMapping("b.yaml", "B", "external-to-core");
        writeMapping("a.yaml", "A", "external-to-core");
        Files.writeString(tmp.resolve("mappings/readme.txt"), "ignored");

        List<InputMapping> mappings = loadConfig().mappings();

        assertEquals("A", mappings.getFirst().revInput());
        assertEquals(MappingDirection.EXTERNAL_TO_CORE, mappings.getFirst().direction());
        assertEquals("B", mappings.get(1).revInput());
    }

    @Test
    void shouldFailOnInvalidMappingDirection() throws Exception {
        writeConnectorYaml("5s", "mappings");
        Files.createDirectories(tmp.resolve("mappings"));
        writeMapping("bad.yaml", "A", "sideways");

        assertThrows(IllegalArgumentException.class, this::loadConfig);
    }

    @Test
    void shouldFailWhenMappingDirectoryIsEmpty() throws Exception {
        writeConnectorYaml("5s", "mappings");
        Files.createDirectories(tmp.resolve("mappings"));

        assertThrows(IllegalArgumentException.class, this::loadConfig);
    }

    private ConnectorConfigDirectory configDirectory() {
        return ConnectorConfigDirectory.at(tmp);
    }

    private MaConnectorConfig loadConfig() throws Exception {
        return new MaConnectorConfigLoader().load(configDirectory());
    }

    private void writeMapping(String fileName, String revInput, String direction) throws Exception {
        Files.writeString(tmp.resolve("mappings/" + fileName), """
                revInput: "%s"
                timeSeriesId: "%s"
                direction: "%s"
                """.formatted(revInput, UUID.fromString("11111111-1111-1111-1111-111111111111"), direction));
    }

    private void writeDefaultMapping() throws Exception {
        Files.createDirectories(tmp.resolve("mappings"));
        writeMapping("default.yaml", "A", "external-to-core");
    }

    private void writeConnectorYaml(String delay, String mappingsDirectory) throws Exception {
        Files.writeString(tmp.resolve("connector.yaml"), """
                core:
                  baseUrl: "http://127.0.0.1:8080/"
                  authentication:
                    tokenUrl: "http://keycloak.local/token"
                    clientId: "ma-client"
                    clientSecret: "secret"
                polling:
                  interval: "%s"
                %s""".formatted(
                delay,
                mappingsDirectory == null
                        ? ""
                        : "mappings:\n  directory: \"" + mappingsDirectory + "\"\n"));
    }
}
