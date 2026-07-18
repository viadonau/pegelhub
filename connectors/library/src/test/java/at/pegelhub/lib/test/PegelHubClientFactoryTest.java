package at.pegelhub.lib.test;

import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.PegelHubClientFactory;
import at.pegelhub.lib.config.CoreAuthentication;
import at.pegelhub.lib.config.CoreConnection;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class PegelHubClientFactoryTest {
    @Test
    void createsClientFromExplicitCoreConnection() throws Exception {
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        try (var httpClients = mockStatic(HttpClients.class)) {
            httpClients.when(HttpClients::createDefault).thenReturn(httpClient);
            CoreConnection connection = new CoreConnection(
                    URI.create("http://localhost:8080/").toURL(),
                    new CoreAuthentication("http://keycloak.local/token", "connector", "secret"));

            PegelHubClient client = PegelHubClientFactory.http().create(connection);

            assertNotNull(client);
            client.close();
            verify(httpClient).close();
        }
    }

    @Test
    void rejectsInvalidAuthenticationBeforeAllocatingHttpClient() {
        assertThrows(IllegalArgumentException.class,
                () -> new CoreAuthentication(" ", "connector", "secret"));
    }
}
