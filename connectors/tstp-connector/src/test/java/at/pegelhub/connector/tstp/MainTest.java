package at.pegelhub.connector.tstp;

import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.runtime.ConnectorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MainTest {
    private static final UUID TIME_SERIES_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @TempDir
    Path tmp;

    @Test
    void shouldUseDefaultConfigDirWhenNoArgumentIsProvided() throws Exception {
        assertEquals("/app/config", ConnectorContext.fromArgs(new String[0]).configDir().toString());
    }

    @Test
    void shouldResolveConnectorOptionsFromExplicitConfigDir() throws Exception {
        writeConfig("core-to-external");

        ConnectorContext context = ConnectorContext.fromArgs(new String[]{tmp.toString()});
        ConnectorOptions options = new TstpConnectorModule().getConnectorOptions(context);

        assertEquals("http://127.0.0.1:8081/", options.coreConnection().baseUrl().toString());
        assertEquals("http://keycloak.local/token", options.coreConnection().credentials().tokenUrl());
        assertEquals("connector", options.coreConnection().credentials().clientId());
        assertEquals("secret", options.coreConnection().credentials().clientSecret());
        assertEquals("127.0.0.2", options.tstpAddress());
        assertEquals(8030, options.tstpPort());
        assertEquals(Duration.ofSeconds(10), options.readDelay());
        assertEquals(TIME_SERIES_ID, options.timeSeriesId());
        assertEquals(77, options.stationId());
        assertEquals(MappingDirection.CORE_TO_EXTERNAL, options.direction());
    }

    @Test
    void failsWhenNoMappingExists() throws Exception {
        writeConnectorYaml();
        Files.createDirectories(tmp.resolve("mappings"));

        ConnectorContext context = ConnectorContext.fromArgs(new String[]{tmp.toString()});

        assertThrows(IllegalArgumentException.class, () -> new TstpConnectorModule().getConnectorOptions(context));
    }

    @Test
    void failsWhenMoreThanOneMappingExists() throws Exception {
        writeConfig("external-to-core");
        Files.writeString(tmp.resolve("mappings/other.yaml"), """
                timeSeriesId: "22222222-2222-2222-2222-222222222222"
                stationId: 78
                direction: "core-to-external"
                """);

        ConnectorContext context = ConnectorContext.fromArgs(new String[]{tmp.toString()});

        assertThrows(IllegalArgumentException.class, () -> new TstpConnectorModule().getConnectorOptions(context));
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
        Files.writeString(tmp.resolve("connector.yaml"), """
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
                  port: 8030
                """);
    }
}
