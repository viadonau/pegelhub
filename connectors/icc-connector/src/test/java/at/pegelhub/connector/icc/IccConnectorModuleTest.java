package at.pegelhub.connector.icc;

import at.pegelhub.lib.CoreConnection;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.PegelHubClientFactory;
import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.runtime.ConnectorBootstrap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class IccConnectorModuleTest {
    @TempDir
    Path tmp;

    @Test
    void shouldUseDefaultConfigDirWhenNoArgumentIsProvided() throws Exception {
        assertEquals("/app/config", ConnectorBootstrap.fromArgs(new String[0]).configDirectory().toString());
    }

    @Test
    void shouldResolveAllConfigFilesFromExplicitConfigDir() throws Exception {
        String configDir = "/tmp/icc-config";
        ConnectorBootstrap context = ConnectorBootstrap.fromArgs(new String[]{configDir});

        assertEquals(Path.of(configDir, "connector.yaml").toString(),
                context.resolve("connector.yaml").toString());
        assertEquals(Path.of(configDir, "mappings").toString(),
                context.resolve("mappings").toString());
    }

    @Test
    void shouldLoadOptionsFromConnectorYamlAndMappings() throws Exception {
        UUID first = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID second = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID firstExternal = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID secondExternal = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        writeConfig(first, second);

        IccConnectorSettings options = new IccConnectorModule()
                .getConnectorSettings(ConnectorBootstrap.fromArgs(new String[]{tmp.toString()}));

        assertEquals("http://core.local:8080/", options.coreConnection().baseUrl().toString());
        assertEquals("core-client", options.coreConnection().credentials().clientId());
        assertEquals("http://external.local:8080/", options.externalConnection().baseUrl().toString());
        assertEquals("external-client", options.externalConnection().credentials().clientId());
        assertEquals(java.time.Duration.ofMinutes(15), options.pollInterval());
        assertEquals(List.of(
                new IccMapping(first, firstExternal, MappingDirection.CORE_TO_EXTERNAL),
                new IccMapping(second, secondExternal, MappingDirection.EXTERNAL_TO_CORE)), options.mappings());
    }

    @Test
    void shouldCloseCoreClientWhenExternalClientCreationFails() throws Exception {
        UUID first = UUID.fromString("11111111-1111-1111-1111-111111111111");
        writeConfig(first);
        PegelHubClient core = mock(PegelHubClient.class);

        var clientFactory = (PegelHubClientFactory) connection -> {
                        if (connection.baseUrl().toString().contains("core.local")) {
                            return core;
                        }
                        throw new RuntimeException("external failed");
                    };

        assertThrows(RuntimeException.class, () -> new IccConnectorModule()
                    .define(ConnectorBootstrap.forDirectory(tmp, clientFactory)));

        verify(core).close();
    }

    private void writeConfig(UUID... ids) throws Exception {
        Files.writeString(tmp.resolve("connector.yaml"), """
                core:
                  baseUrl: "http://core.local:8080/"
                keycloak:
                  tokenUrl: "http://core-keycloak.local/token"
                  clientId: "core-client"
                  clientSecret: "core-secret"
                externalCore:
                  core:
                    baseUrl: "http://external.local:8080/"
                  keycloak:
                    tokenUrl: "http://external-keycloak.local/token"
                    clientId: "external-client"
                    clientSecret: "external-secret"
                schedule:
                  delay: "15m"
                """);
        Files.createDirectories(tmp.resolve("mappings"));
        for (int i = 0; i < ids.length; i++) {
            String direction = i % 2 == 0 ? "core-to-external" : "external-to-core";
            UUID externalId = i % 2 == 0
                    ? UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
                    : UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
            Files.writeString(tmp.resolve("mappings/" + i + ".yaml"), """
                    timeSeriesId: "%s"
                    externalTimeSeriesId: "%s"
                    direction: "%s"
                    """.formatted(ids[i], externalId, direction));
        }
    }
}
