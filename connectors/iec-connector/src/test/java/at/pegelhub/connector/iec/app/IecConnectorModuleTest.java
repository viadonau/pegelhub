package at.pegelhub.connector.iec.app;

import at.pegelhub.connector.iec.config.IecConnectorConfig;
import at.pegelhub.connector.iec.config.IecConnectorConfigLoader;
import at.pegelhub.connector.iec.datapoints.DataPointMapping;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class IecConnectorModuleTest {
    @TempDir
    Path tmp;

    @Test
    void shouldLoadCheckedInExampleConfiguration() throws Exception {
        IecConnectorConfig config = new IecConnectorConfigLoader().load(
                ConnectorConfigDirectory.at(Path.of("examples/config")));

        assertEquals("127.0.0.1", config.server().host());
        assertEquals(4, config.mappings().size());
    }

    @Test
    void validationModeChecksConfigWithoutOpeningConnections() throws Exception {
        new IecConnectorModule().validate(ConnectorConfigDirectory.at(Path.of("examples/config")));
    }

    @Test
    void shouldLoadAllConfigFieldsFromConnectorYaml() throws Exception {
        writeConnectorYaml("15s", "mappings");
        writeDefaultMapping();

        IecConnectorConfig config = loadConfig();

        assertEquals("http://core.local:8080/", config.coreConnection().baseUrl().toString());
        assertEquals("iec-client", config.coreConnection().authentication().clientId());
        assertEquals("127.0.0.1", config.server().host());
        assertEquals(2404, config.server().port());
        assertEquals(1, config.server().commonAddress());
        assertEquals(Duration.ofSeconds(15), config.pollInterval());
        assertEquals(1, config.mappings().size());
    }

    @Test
    void shouldDefaultMappingsDirFromSharedConfigHelper() throws Exception {
        writeConnectorYaml("2M", null);
        writeDefaultMapping();

        IecConnectorConfig config = loadConfig();

        assertEquals(Duration.ofMinutes(2), config.pollInterval());
        assertEquals(1, config.mappings().size());
    }

    @Test
    void shouldFailWhenRequiredConfigSectionIsMissing() throws Exception {
        Files.writeString(tmp.resolve("connector.yaml"), """
                core:
                  baseUrl: "http://core.local:8080/"
                  authentication:
                    tokenUrl: "http://keycloak.local/token"
                    clientId: "iec-client"
                    clientSecret: "secret"
                polling:
                  interval: "10s"
                """);

        Exception ex = assertThrows(Exception.class, this::loadConfig);
        assertTrue(ex.getMessage().contains("iec"));
    }

    @Test
    void shouldFailWhenProtocolPortIsInvalid() throws Exception {
        writeConnectorYaml("15s", "mappings", 70000, 1);

        Exception ex = assertThrows(Exception.class, this::loadConfig);
        assertTrue(ex.getMessage().contains("iec.server.port"));
    }

    @Test
    void shouldFailWhenCommonAddressIsInvalid() throws Exception {
        writeConnectorYaml("15s", "mappings", 2404, 0);

        Exception ex = assertThrows(Exception.class, this::loadConfig);
        assertTrue(ex.getMessage().contains("iec.server.commonAddress"));
    }

    @Test
    void shouldLoadMappingsInSortedOrder() throws Exception {
        writeConnectorYaml("15s", "mappings");
        Files.createDirectories(tmp.resolve("mappings"));
        writeMapping("b.yaml", 2, "core-to-external");
        writeMapping("a.yaml", 1, "external-to-core");
        Files.writeString(tmp.resolve("mappings/readme.txt"), "ignored");

        List<DataPointMapping> mappings = loadConfig().mappings();

        assertEquals(1, mappings.getFirst().iecIoa());
        assertEquals(MappingDirection.EXTERNAL_TO_CORE, mappings.getFirst().direction());
        assertEquals(2, mappings.get(1).iecIoa());
        assertEquals(MappingDirection.CORE_TO_EXTERNAL, mappings.get(1).direction());
    }

    @Test
    void shouldFailOnInvalidMappingDirection() throws Exception {
        writeConnectorYaml("15s", "mappings");
        Files.createDirectories(tmp.resolve("mappings"));
        writeMapping("bad.yaml", 1, "sideways");

        assertThrows(IllegalArgumentException.class, this::loadConfig);
    }

    @Test
    void shouldFailWhenMappingDirectoryIsEmpty() throws Exception {
        writeConnectorYaml("15s", "mappings");
        Files.createDirectories(tmp.resolve("mappings"));

        assertThrows(IllegalArgumentException.class, this::loadConfig);
    }

    private ConnectorConfigDirectory configDirectory() {
        return ConnectorConfigDirectory.at(tmp);
    }

    private IecConnectorConfig loadConfig() throws Exception {
        return new IecConnectorConfigLoader().load(configDirectory());
    }

    private void writeMapping(String fileName, int ioa, String direction) throws Exception {
        Files.writeString(tmp.resolve("mappings/" + fileName), """
                iecIoa: %d
                timeSeriesId: "%s"
                direction: "%s"
                """.formatted(ioa, UUID.fromString("11111111-1111-1111-1111-111111111111"), direction));
    }

    private void writeDefaultMapping() throws Exception {
        Files.createDirectories(tmp.resolve("mappings"));
        writeMapping("default.yaml", 1, "external-to-core");
    }

    private void writeConnectorYaml(String delay, String mappingsDirectory) throws Exception {
        writeConnectorYaml(delay, mappingsDirectory, 2404, 1);
    }

    private void writeConnectorYaml(
            String delay,
            String mappingsDirectory,
            int port,
            int commonAddress
    ) throws Exception {
        Files.writeString(tmp.resolve("connector.yaml"), """
                core:
                  baseUrl: "http://core.local:8080/"
                  authentication:
                    tokenUrl: "http://keycloak.local/token"
                    clientId: "iec-client"
                    clientSecret: "secret"
                polling:
                  interval: "%s"
                %siec:
                  server:
                    host: "127.0.0.1"
                    port: %d
                    commonAddress: %d
                """.formatted(
                delay,
                mappingsDirectory == null
                        ? ""
                        : "mappings:\n  directory: \"" + mappingsDirectory + "\"\n",
                port,
                commonAddress));
    }
}
