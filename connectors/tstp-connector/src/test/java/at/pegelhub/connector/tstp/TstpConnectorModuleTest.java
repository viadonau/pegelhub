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
    private static final UUID FIRST_SERIES = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SECOND_SERIES = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @TempDir
    Path configDirectory;

    @Test
    void loadsCheckedInExampleConfiguration() throws Exception {
        TstpConnectorConfig config = new TstpConnectorConfigLoader().load(
                ConnectorConfigDirectory.at(Path.of("examples/config")));

        assertEquals("127.0.0.1", config.server().host());
        assertEquals(123, config.mappings().getFirst().stationId());
    }

    @Test
    void loadsSortedMixedDirectionMappings() throws Exception {
        writeConnectorYaml(8030);
        writeMapping("20-outbound.yaml", SECOND_SERIES, 78, "core-to-external");
        writeMapping("10-inbound.yaml", FIRST_SERIES, 77, "external-to-core");

        TstpConnectorConfig config = loadConfig();

        assertEquals("127.0.0.2", config.server().host());
        assertEquals(8030, config.server().port());
        assertEquals(Duration.ofSeconds(10), config.pollInterval());
        assertEquals(2, config.mappings().size());
        assertEquals(MappingDirection.EXTERNAL_TO_CORE, config.mappings().get(0).direction());
        assertEquals(MappingDirection.CORE_TO_EXTERNAL, config.mappings().get(1).direction());
    }

    @Test
    void rejectsNoMappings() throws Exception {
        writeConnectorYaml(8030);
        Files.createDirectories(configDirectory.resolve("mappings"));

        assertThrows(IllegalArgumentException.class, this::loadConfig);
    }

    @Test
    void rejectsDuplicateOutboundTarget() throws Exception {
        writeConnectorYaml(8030);
        writeMapping("one.yaml", FIRST_SERIES, 77, "core-to-external");
        writeMapping("two.yaml", SECOND_SERIES, 77, "core-to-external");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, this::loadConfig);

        assertTrue(error.getMessage().contains("two.yaml"));
        assertTrue(error.getMessage().contains("target station 77"));
    }

    @Test
    void rejectsFeedbackCyclesAcrossMultipleMappings() throws Exception {
        writeConnectorYaml(8030);
        writeMapping("01-a-to-station.yaml", FIRST_SERIES, 77, "core-to-external");
        writeMapping("02-station-to-b.yaml", SECOND_SERIES, 77, "external-to-core");
        writeMapping("03-b-to-station.yaml", SECOND_SERIES, 78, "core-to-external");
        writeMapping("04-station-to-a.yaml", FIRST_SERIES, 78, "external-to-core");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, this::loadConfig);

        assertTrue(error.getMessage().contains("04-station-to-a.yaml"));
        assertTrue(error.getMessage().contains("feedback cycle"));
    }

    @Test
    void rejectsInvalidPort() throws Exception {
        writeConnectorYaml(0);

        Exception error = assertThrows(Exception.class, this::loadConfig);

        assertTrue(error.getMessage().contains("tstp.server.port"));
    }

    private TstpConnectorConfig loadConfig() throws Exception {
        return new TstpConnectorConfigLoader().load(ConnectorConfigDirectory.at(configDirectory));
    }

    private void writeConnectorYaml(int port) throws Exception {
        Files.writeString(configDirectory.resolve("connector.yaml"), """
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

    private void writeMapping(
            String fileName,
            UUID timeSeriesId,
            int stationId,
            String direction
    ) throws Exception {
        Files.createDirectories(configDirectory.resolve("mappings"));
        Files.writeString(configDirectory.resolve("mappings").resolve(fileName), """
                timeSeriesId: "%s"
                stationId: %d
                direction: "%s"
                """.formatted(timeSeriesId, stationId, direction));
    }
}
