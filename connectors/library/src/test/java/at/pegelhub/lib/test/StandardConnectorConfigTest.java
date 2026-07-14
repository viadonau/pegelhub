package at.pegelhub.lib.test;

import at.pegelhub.lib.config.CoreConfig;
import at.pegelhub.lib.config.CoreEndpointConfig;
import at.pegelhub.lib.config.KeycloakConfig;
import at.pegelhub.lib.config.ScheduleConfig;
import at.pegelhub.lib.config.StandardConnectorConfig;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StandardConnectorConfigTest {
    @Test
    void buildsCoreConnectionFromSharedEndpointConfig() throws MalformedURLException {
        CoreEndpointConfig endpoint = endpoint("http://core.local:8080/");

        var connection = endpoint.connection();

        assertEquals("http://core.local:8080/", connection.baseUrl().toExternalForm());
        assertEquals("http://keycloak.local/token", connection.credentials().tokenUrl());
        assertEquals("connector-client", connection.credentials().clientId());
        assertEquals("secret", connection.credentials().clientSecret());
    }

    @Test
    void buildsCoreConnectionFromStandardConnectorConfigEndpoint() throws MalformedURLException {
        StandardConnectorConfig config = new TestConnectorConfig(
                endpoint("http://core.local:8080/"),
                new ScheduleConfig("30s"),
                "mappings");

        var connection = config.coreConnection();

        assertEquals("http://core.local:8080/", connection.baseUrl().toExternalForm());
        assertEquals("connector-client", connection.credentials().clientId());
        assertEquals("mappings", config.mappingsDirectory());
        assertEquals(java.time.Duration.ofSeconds(30), config.scheduleInterval());
    }

    private static CoreEndpointConfig endpoint(String coreUrl) throws MalformedURLException {
        return new CoreEndpointConfig(
                new CoreConfig(URI.create(coreUrl).toURL()),
                new KeycloakConfig("http://keycloak.local/token", "connector-client", "secret"));
    }

    private record TestConnectorConfig(
            CoreEndpointConfig coreEndpoint,
            ScheduleConfig schedule,
            String mappingsDir) implements StandardConnectorConfig {
        @Override
        public CoreConfig core() {
            return coreEndpoint.core();
        }

        @Override
        public KeycloakConfig keycloak() {
            return coreEndpoint.keycloak();
        }
    }
}
