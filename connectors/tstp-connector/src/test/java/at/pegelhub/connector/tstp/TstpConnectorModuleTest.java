package at.pegelhub.connector.tstp;

import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.runtime.ConnectorBootstrap;
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
    void loadsSortedMixedDirectionMappings() throws Exception {
        writeConnectorYaml(8030);
        writeMapping("20-outbound.yaml", SECOND_SERIES, 78, "core-to-external", true);
        writeMapping("10-inbound.yaml", FIRST_SERIES, 77, "external-to-core", false);

        TstpConnectorSettings settings = module().getConnectorSettings(bootstrap());

        assertEquals("127.0.0.2", settings.address());
        assertEquals(8030, settings.port());
        assertEquals(Duration.ofSeconds(10), settings.pollInterval());
        assertEquals(2, settings.mappings().size());
        assertEquals("10-inbound.yaml", settings.mappings().get(0).fileName());
        assertEquals(MappingDirection.EXTERNAL_TO_CORE, settings.mappings().get(0).value().direction());
        assertEquals("20-outbound.yaml", settings.mappings().get(1).fileName());
        assertTrue(settings.mappings().get(1).value().verifyRoundTrip());
    }

    @Test
    void rejectsNoMappings() throws Exception {
        writeConnectorYaml(8030);
        Files.createDirectories(configDirectory.resolve("mappings"));

        assertThrows(IllegalArgumentException.class, () -> module().getConnectorSettings(bootstrap()));
    }

    @Test
    void rejectsDuplicateOutboundTarget() throws Exception {
        writeConnectorYaml(8030);
        writeMapping("one.yaml", FIRST_SERIES, 77, "core-to-external", false);
        writeMapping("two.yaml", SECOND_SERIES, 77, "core-to-external", false);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> module().getConnectorSettings(bootstrap()));

        assertTrue(error.getMessage().contains("two.yaml"));
        assertTrue(error.getMessage().contains("target station 77"));
    }

    @Test
    void rejectsRoundTripVerificationForInboundMapping() throws Exception {
        writeConnectorYaml(8030);
        writeMapping("inbound.yaml", FIRST_SERIES, 77, "external-to-core", true);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> module().getConnectorSettings(bootstrap()));

        assertTrue(error.getMessage().contains("verifyRoundTrip"));
    }

    @Test
    void rejectsInvalidPort() throws Exception {
        writeConnectorYaml(0);

        Exception error = assertThrows(Exception.class, () -> module().getConnectorSettings(bootstrap()));
        assertTrue(error.getMessage().contains("tstp.port"));
    }

    private TstpConnectorModule module() {
        return new TstpConnectorModule();
    }

    private ConnectorBootstrap bootstrap() {
        return ConnectorBootstrap.forDirectory(configDirectory, connection -> {
            throw new AssertionError("Configuration loading must not open a Core client");
        });
    }

    private void writeConnectorYaml(int port) throws Exception {
        Files.writeString(configDirectory.resolve("connector.yaml"), """
                core:
                  baseUrl: "http://127.0.0.1:8081/"
                keycloak:
                  tokenUrl: "http://keycloak.local/token"
                  clientId: "connector"
                  clientSecret: "secret"
                schedule:
                  delay: "10s"
                tstp:
                  address: "127.0.0.2"
                  port: %d
                """.formatted(port));
    }

    private void writeMapping(
            String fileName,
            UUID timeSeriesId,
            int stationId,
            String direction,
            boolean verifyRoundTrip) throws Exception {
        Files.createDirectories(configDirectory.resolve("mappings"));
        Files.writeString(configDirectory.resolve("mappings").resolve(fileName), """
                timeSeriesId: "%s"
                stationId: %d
                direction: "%s"
                verifyRoundTrip: %s
                """.formatted(timeSeriesId, stationId, direction, verifyRoundTrip));
    }
}
