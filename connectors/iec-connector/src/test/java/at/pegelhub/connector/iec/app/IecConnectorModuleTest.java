package at.pegelhub.connector.iec.app;

import at.pegelhub.connector.iec.config.ConnectorOptions;
import at.pegelhub.connector.iec.datapoints.DataPointMapping;
import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.runtime.ConnectorContext;
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
    void shouldLoadAllConfigFieldsFromConnectorYaml() throws Exception {
        writeConnectorYaml("15s", "mappings");

        ConnectorOptions opt = new IecConnectorModule()
                .getConnectorOptions(ConnectorContext.fromArgs(new String[]{tmp.toString()}));

        assertEquals("http://core.local:8080/", opt.coreConnection().baseUrl().toString());
        assertEquals("iec-client", opt.coreConnection().credentials().clientId());
        assertEquals("mappings", opt.mappingsDir());
        assertEquals("127.0.0.1", opt.iecHost().getHostAddress());
        assertEquals(2404, opt.iecPort());
        assertEquals(1, opt.commonAddress());
        assertEquals(Duration.ofSeconds(15), opt.delay());
    }

    @Test
    void shouldDefaultMappingsDirFromSharedConfigHelper() throws Exception {
        writeConnectorYaml("2M", null);

        ConnectorOptions opt = new IecConnectorModule()
                .getConnectorOptions(ConnectorContext.fromArgs(new String[]{tmp.toString()}));

        assertEquals("mappings", opt.mappingsDir());
        assertEquals(Duration.ofMinutes(2), opt.delay());
    }

    @Test
    void shouldFailWhenRequiredConfigSectionIsMissing() throws Exception {
        Files.writeString(tmp.resolve("connector.yaml"), """
                core:
                  baseUrl: "http://core.local:8080/"
                keycloak:
                  tokenUrl: "http://keycloak.local/token"
                  clientId: "iec-client"
                  clientSecret: "secret"
                schedule:
                  delay: "10s"
                """);

        Exception ex = assertThrows(Exception.class, () -> new IecConnectorModule()
                .getConnectorOptions(ConnectorContext.fromArgs(new String[]{tmp.toString()})));
        assertTrue(ex.getMessage().contains("iec"));
    }

    @Test
    void shouldFailWhenProtocolPortIsInvalid() throws Exception {
        writeConnectorYaml("15s", "mappings", 70000, 1);

        Exception ex = assertThrows(Exception.class, () -> new IecConnectorModule()
                .getConnectorOptions(ConnectorContext.fromArgs(new String[]{tmp.toString()})));
        assertTrue(ex.getMessage().contains("iec.port"));
    }

    @Test
    void shouldFailWhenCommonAddressIsInvalid() throws Exception {
        writeConnectorYaml("15s", "mappings", 2404, 0);

        Exception ex = assertThrows(Exception.class, () -> new IecConnectorModule()
                .getConnectorOptions(ConnectorContext.fromArgs(new String[]{tmp.toString()})));
        assertTrue(ex.getMessage().contains("iec.commonAddress"));
    }

    @Test
    void shouldLoadMappingsThroughConnectorContextInSortedOrder() throws Exception {
        Files.createDirectories(tmp.resolve("mappings"));
        writeMapping("b.yaml", 2, "core-to-external");
        writeMapping("a.yaml", 1, "external-to-core");
        Files.writeString(tmp.resolve("mappings/readme.txt"), "ignored");

        List<DataPointMapping> mappings = new IecConnectorModule()
                .loadMappings(ConnectorContext.fromArgs(new String[]{tmp.toString()}), "mappings");

        assertEquals(1, mappings.getFirst().iecIoa());
        assertEquals(MappingDirection.EXTERNAL_TO_CORE, mappings.getFirst().direction());
        assertEquals(2, mappings.get(1).iecIoa());
        assertEquals(MappingDirection.CORE_TO_EXTERNAL, mappings.get(1).direction());
    }

    @Test
    void shouldFailOnInvalidMappingDirection() throws Exception {
        Files.createDirectories(tmp.resolve("mappings"));
        writeMapping("bad.yaml", 1, "sideways");

        assertThrows(IllegalArgumentException.class, () -> new IecConnectorModule()
                .loadMappings(ConnectorContext.fromArgs(new String[]{tmp.toString()}), "mappings"));
    }

    @Test
    void shouldFailWhenMappingDirectoryIsEmpty() throws Exception {
        Files.createDirectories(tmp.resolve("mappings"));

        assertThrows(IllegalArgumentException.class, () -> new IecConnectorModule()
                .loadMappings(ConnectorContext.fromArgs(new String[]{tmp.toString()}), "mappings"));
    }

    private void writeMapping(String fileName, int ioa, String direction) throws Exception {
        Files.writeString(tmp.resolve("mappings/" + fileName), """
                iecIoa: %d
                timeSeriesId: "%s"
                direction: "%s"
                """.formatted(ioa, UUID.fromString("11111111-1111-1111-1111-111111111111"), direction));
    }

    private void writeConnectorYaml(String delay, String mappingsDir) throws Exception {
        writeConnectorYaml(delay, mappingsDir, 2404, 1);
    }

    private void writeConnectorYaml(String delay, String mappingsDir, int port, int commonAddress) throws Exception {
        Files.writeString(tmp.resolve("connector.yaml"), """
                core:
                  baseUrl: "http://core.local:8080/"
                keycloak:
                  tokenUrl: "http://keycloak.local/token"
                  clientId: "iec-client"
                  clientSecret: "secret"
                schedule:
                  delay: "%s"
                %siec:
                  address: "127.0.0.1"
                  port: %d
                  commonAddress: %d
                """.formatted(delay, mappingsDir == null ? "" : "mappingsDir: \"" + mappingsDir + "\"\n", port, commonAddress));
    }
}
