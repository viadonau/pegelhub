package at.pegelhub.connector.ftp;

import at.pegelhub.connector.ftp.fileparsing.ParserType;
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
        writeConfig("""
                timeSeriesId: "11111111-1111-1111-1111-111111111111"
                stationId: 123
                parameter: "Abfluss"
                direction: "external-to-core"
                """);

        ConnectorOptions options = new FtpConnectorModule().getConnectorOptions(ConnectorContext.fromArgs(new String[]{tmp.toString()}));

        assertEquals("http://127.0.0.1:8081/", options.coreConnection().baseUrl().toString());
        assertEquals("connector", options.coreConnection().credentials().clientId());
        assertEquals("127.0.0.2", options.ftpAddress().getHostAddress());
        assertEquals(21, options.ftpPort());
        assertEquals("test-user", options.username());
        assertEquals("test-pass", options.password());
        assertEquals("/incoming", options.path());
        assertEquals(ParserType.ZRXP, options.parserType());
        assertEquals("Abfluss", options.parameter());
        assertEquals(Duration.ofMinutes(15), options.readDelay());
        assertEquals(TIME_SERIES_ID, options.timeSeriesId());
        assertEquals(123, options.stationId());
    }

    @Test
    void shouldRejectZeroOrMultipleMappings() throws Exception {
        writeConnectorYaml();
        Files.createDirectories(tmp.resolve("mappings"));

        FtpConnectorModule module = new FtpConnectorModule();
        ConnectorContext context = ConnectorContext.fromArgs(new String[]{tmp.toString()});
        assertThrows(IllegalArgumentException.class, () -> module.getConnectorOptions(context));

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

        assertThrows(IllegalArgumentException.class, () -> module.getConnectorOptions(context));
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

        FtpConnectorModule module = new FtpConnectorModule();
        ConnectorContext context = ConnectorContext.fromArgs(new String[]{tmp.toString()});

        assertThrows(IllegalArgumentException.class, () -> module.getConnectorOptions(context));
    }

    private void writeConfig(String mappingYaml) throws Exception {
        writeConnectorYaml();
        Files.createDirectories(tmp.resolve("mappings"));
        Files.writeString(tmp.resolve("mappings/station.yaml"), mappingYaml);
    }

    private void writeConnectorYaml() throws Exception {
        writeConnectorYaml("zrxp");
    }

    private void writeConnectorYaml(String parserType) throws Exception {
        Files.writeString(tmp.resolve("connector.yaml"), """
                core:
                  baseUrl: "http://127.0.0.1:8081/"
                keycloak:
                  tokenUrl: "http://keycloak.local/token"
                  clientId: "connector"
                  clientSecret: "secret"
                schedule:
                  delay: "15m"
                ftp:
                  address: "127.0.0.2"
                  port: 21
                  user: "test-user"
                  password: "test-pass"
                  path: "/incoming"
                  parserType: "%s"
                """.formatted(parserType));
    }
}
