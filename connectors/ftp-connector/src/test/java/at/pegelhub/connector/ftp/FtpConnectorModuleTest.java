package at.pegelhub.connector.ftp;

import at.pegelhub.connector.ftp.config.FtpConnectorConfig;
import at.pegelhub.connector.ftp.config.FtpConnectorConfigLoader;
import at.pegelhub.connector.ftp.fileparsing.ParserType;
import at.pegelhub.lib.config.ConnectorConfigDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FtpConnectorModuleTest {
    private static final UUID TIME_SERIES_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @TempDir
    Path tmp;

    @Test
    void shouldLoadCheckedInExampleConfiguration() throws Exception {
        FtpConnectorConfig config = new FtpConnectorConfigLoader().load(
                ConnectorConfigDirectory.at(Path.of("examples/config")));

        assertEquals("ftp.viadonau.org", config.server().host());
        assertEquals(ParserType.ZRXP, config.source().parserType());
    }

    @Test
    void shouldResolveConnectorConfigFromExplicitConfigDir() throws Exception {
        writeConfig("""
                timeSeriesId: "11111111-1111-1111-1111-111111111111"
                stationId: 123
                parameter: "Abfluss"
                direction: "external-to-core"
                """);

        FtpConnectorConfig config = loadConfig();

        assertEquals("http://127.0.0.1:8081/", config.coreConnection().baseUrl().toString());
        assertEquals("connector", config.coreConnection().authentication().clientId());
        assertEquals("127.0.0.2", config.server().host());
        assertEquals(21, config.server().port());
        assertEquals("test-user", config.server().authentication().username());
        assertEquals("test-pass", config.server().authentication().password());
        assertEquals("/incoming", config.source().directory());
        assertEquals(ParserType.ZRXP, config.source().parserType());
        assertEquals("Abfluss", config.mapping().sourceParameter());
        assertEquals(Duration.ofMinutes(15), config.pollInterval());
        assertEquals(TIME_SERIES_ID, config.mapping().targetTimeSeriesId());
        assertEquals(123, config.mapping().sourceStationId());
    }

    @Test
    void shouldRejectZeroOrMultipleMappings() throws Exception {
        writeConnectorYaml();
        Files.createDirectories(tmp.resolve("mappings"));

        ConnectorConfigDirectory configDirectory = configDirectory();
        FtpConnectorConfigLoader loader = new FtpConnectorConfigLoader();
        assertThrows(IllegalArgumentException.class, () -> loader.load(configDirectory));

        Files.writeString(tmp.resolve("mappings/a.yaml"), """
                timeSeriesId: "11111111-1111-1111-1111-111111111111"
                stationId: 123
                direction: "external-to-core"
                """);
        Files.writeString(tmp.resolve("mappings/b.yaml"), """
                timeSeriesId: "22222222-2222-2222-2222-222222222222"
                stationId: 456
                direction: "external-to-core"
                """);

        assertThrows(IllegalArgumentException.class, () -> loader.load(configDirectory));
    }

    @Test
    void shouldRejectUnknownParserType() throws Exception {
        writeConnectorYaml("unknown");
        Files.createDirectories(tmp.resolve("mappings"));
        Files.writeString(tmp.resolve("mappings/station.yaml"), """
                timeSeriesId: "11111111-1111-1111-1111-111111111111"
                stationId: 123
                direction: "external-to-core"
                """);

        ConnectorConfigDirectory configDirectory = configDirectory();

        assertThrows(Exception.class, () -> new FtpConnectorConfigLoader().load(configDirectory));
    }

    @Test
    void shouldRejectInvalidProtocolPort() throws Exception {
        writeConnectorYaml("zrxp", 0);

        ConnectorConfigDirectory configDirectory = configDirectory();

        Exception ex = assertThrows(Exception.class,
                () -> new FtpConnectorConfigLoader().load(configDirectory));
        assertTrue(ex.getMessage().contains("ftp.server.port"));
    }

    private void writeConfig(String mappingYaml) throws Exception {
        writeConnectorYaml();
        Files.createDirectories(tmp.resolve("mappings"));
        Files.writeString(tmp.resolve("mappings/station.yaml"), mappingYaml);
    }

    private ConnectorConfigDirectory configDirectory() {
        return ConnectorConfigDirectory.at(tmp);
    }

    private FtpConnectorConfig loadConfig() throws Exception {
        return new FtpConnectorConfigLoader().load(configDirectory());
    }

    private void writeConnectorYaml() throws Exception {
        writeConnectorYaml("zrxp");
    }

    private void writeConnectorYaml(String parserType) throws Exception {
        writeConnectorYaml(parserType, 21);
    }

    private void writeConnectorYaml(String parserType, int port) throws Exception {
        Files.writeString(tmp.resolve("connector.yaml"), """
                core:
                  baseUrl: "http://127.0.0.1:8081/"
                  authentication:
                    tokenUrl: "http://keycloak.local/token"
                    clientId: "connector"
                    clientSecret: "secret"
                polling:
                  interval: "15m"
                ftp:
                  server:
                    host: "127.0.0.2"
                    port: %d
                    authentication:
                      username: "test-user"
                      password: "test-pass"
                  source:
                    directory: "/incoming"
                    parserType: "%s"
                """.formatted(port, parserType));
    }
}
