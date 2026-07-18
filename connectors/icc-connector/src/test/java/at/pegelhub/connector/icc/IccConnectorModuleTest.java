package at.pegelhub.connector.icc;

import at.pegelhub.connector.icc.config.IccConnectorConfig;
import at.pegelhub.connector.icc.config.IccConnectorConfigLoader;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.PegelHubClientFactory;
import at.pegelhub.lib.config.ConnectorConfigDirectory;
import at.pegelhub.lib.config.MappingDirection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class IccConnectorModuleTest {
    @TempDir
    Path tmp;

    @Test
    void shouldLoadCheckedInExampleConfiguration() throws Exception {
        IccConnectorConfig config = new IccConnectorConfigLoader().load(
                ConnectorConfigDirectory.at(Path.of("examples/config")));

        assertEquals("http://core.local:8080/", config.localCore().baseUrl().toString());
        assertEquals("http://external-core.local:8080/", config.remoteCore().baseUrl().toString());
        assertEquals(2, config.mappings().size());
    }

    @Test
    void shouldResolveAllConfigFilesFromExplicitConfigDir() throws Exception {
        String configDir = "/tmp/icc-config";
        ConnectorConfigDirectory configDirectory = ConnectorConfigDirectory.at(Path.of(configDir));

        assertEquals(Path.of(configDir, "connector.yaml").toString(),
                configDirectory.resolve("connector.yaml").toString());
        assertEquals(Path.of(configDir, "mappings").toString(),
                configDirectory.resolve("mappings").toString());
    }

    @Test
    void shouldLoadConfigFromConnectorYamlAndMappings() throws Exception {
        UUID first = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID second = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID firstExternal = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID secondExternal = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        writeConfig(first, second);

        IccConnectorConfig config = new IccConnectorConfigLoader()
                .load(configDirectory());

        assertEquals("http://core.local:8080/", config.localCore().baseUrl().toString());
        assertEquals("core-client", config.localCore().authentication().clientId());
        assertEquals("http://external.local:8080/", config.remoteCore().baseUrl().toString());
        assertEquals("external-client", config.remoteCore().authentication().clientId());
        assertEquals(java.time.Duration.ofMinutes(15), config.pollInterval());
        assertEquals(List.of(
                new IccMapping(first, firstExternal, MappingDirection.CORE_TO_EXTERNAL),
                new IccMapping(second, secondExternal, MappingDirection.EXTERNAL_TO_CORE)), config.mappings());
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
                    .define(configDirectory(), clientFactory));

        verify(core).close();
    }

    private ConnectorConfigDirectory configDirectory() {
        return ConnectorConfigDirectory.at(tmp);
    }

    private void writeConfig(UUID... ids) throws Exception {
        Files.writeString(tmp.resolve("connector.yaml"), """
                localCore:
                  baseUrl: "http://core.local:8080/"
                  authentication:
                    tokenUrl: "http://core-keycloak.local/token"
                    clientId: "core-client"
                    clientSecret: "core-secret"
                remoteCore:
                  baseUrl: "http://external.local:8080/"
                  authentication:
                    tokenUrl: "http://external-keycloak.local/token"
                    clientId: "external-client"
                    clientSecret: "external-secret"
                polling:
                  interval: "15m"
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
