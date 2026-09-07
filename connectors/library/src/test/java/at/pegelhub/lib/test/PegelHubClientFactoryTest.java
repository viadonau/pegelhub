package at.pegelhub.lib.test;

import at.pegelhub.lib.PegelHubClientFactory;
import at.pegelhub.lib.config.CoreAuthentication;
import at.pegelhub.lib.config.CoreConnection;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PegelHubClientFactoryTest {
    @Test
    void createsClientFromExplicitCoreConnection() throws Exception {
        var connection = new CoreConnection(URI.create("http://localhost:8080/").toURL(),
                new CoreAuthentication("http://keycloak.local/token", "connector", "secret"));
        try (var client = PegelHubClientFactory.http().create(connection)) {
            assertNotNull(client);
        }
    }

    @Test
    void rejectsInvalidAuthenticationBeforeAllocatingHttpClient() {
        assertThrows(IllegalArgumentException.class,
                () -> new CoreAuthentication(" ", "connector", "secret"));
    }
}
