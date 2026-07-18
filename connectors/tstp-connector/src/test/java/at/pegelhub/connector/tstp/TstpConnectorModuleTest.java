package at.pegelhub.connector.tstp;

import at.pegelhub.connector.tstp.config.TstpConnectorConfig;
import at.pegelhub.connector.tstp.config.TstpConnectorConfigLoader;
import at.pegelhub.lib.config.ConnectorConfigDirectory;
import at.pegelhub.lib.config.MappingDirection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TstpConnectorModuleTest {
    private static final UUID TIME_SERIES_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @TempDir
    Path tmp;

    @Test
    void shouldLoadCheckedInExampleConfiguration() throws Exception {
        TstpConnectorConfig config = new TstpConnectorConfigLoader().load(
                ConnectorConfigDirectory.at(Path.of("examples/config")));

        assertEquals("127.0.0.1", config.server().host());
        assertEquals(123, config.mapping().stationId());
    }

    @Test
    void shouldResolveConnectorConfigFromExplicitConfigDir() throws Exception {
        writeConfig("core-to-external");

        TstpConnectorConfig config = new TstpConnectorConfigLoader()
                .load(configDirectory());

        assertEquals("http://127.0.0.1:8081/", config.coreConnection().baseUrl().toString());
        assertEquals("http://keycloak.local/token", config.coreConnection().authentication().tokenUrl());
        assertEquals("connector", config.coreConnection().authentication().clientId());
        assertEquals("secret", config.coreConnection().authentication().clientSecret());
        assertEquals("127.0.0.2", config.server().host());
        assertEquals(8030, config.server().port());
        assertEquals(Duration.ofSeconds(10), config.pollInterval());
        assertEquals(TIME_SERIES_ID, config.mapping().timeSeriesId());
        assertEquals(77, config.mapping().stationId());
        assertEquals(MappingDirection.CORE_TO_EXTERNAL, config.mapping().direction());
    }

    @Test
    void failsWhenNoMappingExists() throws Exception {
        writeConnectorYaml();
        Files.createDirectories(tmp.resolve("mappings"));

        ConnectorConfigDirectory configDirectory = configDirectory();

        assertThrows(IllegalArgumentException.class,
                () -> new TstpConnectorConfigLoader().load(configDirectory));
    }

    @Test
    void failsWhenMoreThanOneMappingExists() throws Exception {
        writeConfig("external-to-core");
        Files.writeString(tmp.resolve("mappings/other.yaml"), """
                timeSeriesId: "22222222-2222-2222-2222-222222222222"
                stationId: 78
                direction: "core-to-external"
                """);

        ConnectorConfigDirectory configDirectory = configDirectory();

        assertThrows(IllegalArgumentException.class,
                () -> new TstpConnectorConfigLoader().load(configDirectory));
    }

    @Test
    void failsWhenProtocolPortIsInvalid() throws Exception {
        writeConnectorYaml(0);

        ConnectorConfigDirectory configDirectory = configDirectory();

        Exception ex = assertThrows(Exception.class,
                () -> new TstpConnectorConfigLoader().load(configDirectory));
        assertTrue(ex.getMessage().contains("tstp.server.port"));
    }

    private ConnectorConfigDirectory configDirectory() {
        return ConnectorConfigDirectory.at(tmp);
    }

    private void writeConfig(String direction) throws Exception {
        writeConnectorYaml();
        Files.createDirectories(tmp.resolve("mappings"));
        Files.writeString(tmp.resolve("mappings/station.yaml"), """
                timeSeriesId: "11111111-1111-1111-1111-111111111111"
                stationId: 77
                direction: "%s"
                """.formatted(direction));
    }

    private void writeConnectorYaml() throws Exception {
        writeConnectorYaml(8030);
    }

    private void writeConnectorYaml(int port) throws Exception {
        Files.writeString(tmp.resolve("connector.yaml"), """
                core:
                  baseUrl: "http://127.0.0.1:8081/"
                  authentication:
                    tokenUrl: "http://keycloak.local/token"
                    clientId: "connector"
                    clientSecret: "secret"
                polling:
                  interval: "10s"
                tstp:
                  server:
                    host: "127.0.0.2"
                    port: %d
                """.formatted(port));
    }
}
