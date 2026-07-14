package at.pegelhub.connector.ma;

import at.pegelhub.connector.ma.config.MaConnectorSettings;
import at.pegelhub.connector.ma.core.InputMapping;
import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.runtime.ConnectorBootstrap;
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
    void shouldLoadAllConfigFieldsFromConnectorYaml() throws Exception {
        writeConnectorYaml("5s", "mappings");

        MaConnectorSettings opts = new MaConnectorModule()
                .getConnectorSettings(ConnectorBootstrap.fromArgs(new String[]{tmp.toString()}));

        assertEquals("http://127.0.0.1:8080/", opts.coreConnection().baseUrl().toString());
        assertEquals("ma-client", opts.coreConnection().credentials().clientId());
        assertEquals(Duration.ofSeconds(5), opts.pollInterval());
        assertEquals("mappings", opts.mappingsDirectory());
    }

    @Test
    void shouldParseDelayUnitsCaseInsensitively() throws Exception {
        writeConnectorYaml("2M", null);
        MaConnectorSettings optM = new MaConnectorModule()
                .getConnectorSettings(ConnectorBootstrap.fromArgs(new String[]{tmp.toString()}));
        assertEquals(Duration.ofMinutes(2), optM.pollInterval());

        writeConnectorYaml("1H", null);
        MaConnectorSettings optH = new MaConnectorModule()
                .getConnectorSettings(ConnectorBootstrap.fromArgs(new String[]{tmp.toString()}));
        assertEquals(Duration.ofHours(1), optH.pollInterval());
    }

    @Test
    void shouldFailOnUnknownDelayUnit() throws Exception {
        writeConnectorYaml("10x", null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new MaConnectorModule()
                .getConnectorSettings(ConnectorBootstrap.fromArgs(new String[]{tmp.toString()})));
        assertEquals("schedule.delay must end with s, m, or h", ex.getMessage());
    }

    @Test
    void shouldDefaultMappingsDirFromSharedConfigHelper() throws Exception {
        writeConnectorYaml("1s", null);

        MaConnectorSettings opts = new MaConnectorModule()
                .getConnectorSettings(ConnectorBootstrap.fromArgs(new String[]{tmp.toString()}));

        assertEquals("mappings", opts.mappingsDirectory());
    }

    @Test
    void shouldLoadMappingsThroughConnectorBootstrapInSortedOrder() throws Exception {
        Files.createDirectories(tmp.resolve("mappings"));
        writeMapping("b.yaml", "B", "external-to-core");
        writeMapping("a.yaml", "A", "external-to-core");
        Files.writeString(tmp.resolve("mappings/readme.txt"), "ignored");

        List<InputMapping> mappings = new MaConnectorModule()
                .loadMappings(ConnectorBootstrap.fromArgs(new String[]{tmp.toString()}), "mappings");

        assertEquals("A", mappings.getFirst().revInput());
        assertEquals(MappingDirection.EXTERNAL_TO_CORE, mappings.getFirst().direction());
        assertEquals("B", mappings.get(1).revInput());
    }

    @Test
    void shouldFailOnInvalidMappingDirection() throws Exception {
        Files.createDirectories(tmp.resolve("mappings"));
        writeMapping("bad.yaml", "A", "sideways");

        assertThrows(IllegalArgumentException.class, () -> new MaConnectorModule()
                .loadMappings(ConnectorBootstrap.fromArgs(new String[]{tmp.toString()}), "mappings"));
    }

    @Test
    void shouldFailWhenMappingDirectoryIsEmpty() throws Exception {
        Files.createDirectories(tmp.resolve("mappings"));

        assertThrows(IllegalArgumentException.class, () -> new MaConnectorModule()
                .loadMappings(ConnectorBootstrap.fromArgs(new String[]{tmp.toString()}), "mappings"));
    }

    private void writeMapping(String fileName, String revInput, String direction) throws Exception {
        Files.writeString(tmp.resolve("mappings/" + fileName), """
                revInput: "%s"
                timeSeriesId: "%s"
                direction: "%s"
                """.formatted(revInput, UUID.fromString("11111111-1111-1111-1111-111111111111"), direction));
    }

    private void writeConnectorYaml(String delay, String mappingsDir) throws Exception {
        Files.writeString(tmp.resolve("connector.yaml"), """
                core:
                  baseUrl: "http://127.0.0.1:8080/"
                keycloak:
                  tokenUrl: "http://keycloak.local/token"
                  clientId: "ma-client"
                  clientSecret: "secret"
                schedule:
                  delay: "%s"
                %s""".formatted(delay, mappingsDir == null ? "" : "mappingsDir: \"" + mappingsDir + "\"\n"));
    }
}
