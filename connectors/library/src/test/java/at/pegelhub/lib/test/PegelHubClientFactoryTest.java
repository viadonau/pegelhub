package at.pegelhub.lib.test;

import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.PegelHubClientFactory;
import at.pegelhub.lib.config.CoreAuthentication;
import at.pegelhub.lib.config.CoreConnection;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PegelHubClientFactoryTest {
    @Test
    void createsClientFromExplicitCoreConnection() throws Exception {
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        HttpClientBuilder builder = mock(HttpClientBuilder.class);
        PoolingHttpClientConnectionManager connectionManager = mock(PoolingHttpClientConnectionManager.class);
        PoolingHttpClientConnectionManagerBuilder connectionManagerBuilder =
                mock(PoolingHttpClientConnectionManagerBuilder.class);
        try (var httpClients = mockStatic(HttpClients.class);
             var connectionManagers = mockStatic(PoolingHttpClientConnectionManagerBuilder.class)) {
            httpClients.when(HttpClients::custom).thenReturn(builder);
            connectionManagers.when(PoolingHttpClientConnectionManagerBuilder::create)
                    .thenReturn(connectionManagerBuilder);
            when(connectionManagerBuilder.setDefaultConnectionConfig(
                    org.mockito.ArgumentMatchers.any())).thenReturn(connectionManagerBuilder);
            when(connectionManagerBuilder.build()).thenReturn(connectionManager);
            when(builder.setConnectionManager(connectionManager)).thenReturn(builder);
            when(builder.setDefaultRequestConfig(org.mockito.ArgumentMatchers.any())).thenReturn(builder);
            when(builder.build()).thenReturn(httpClient);
            CoreConnection connection = new CoreConnection(
                    URI.create("http://localhost:8080/").toURL(),
                    new CoreAuthentication("http://keycloak.local/token", "connector", "secret"));

            PegelHubClient client = PegelHubClientFactory.http().create(connection);

            assertNotNull(client);
            client.close();
            verify(connectionManagerBuilder).setDefaultConnectionConfig(
                    org.mockito.ArgumentMatchers.argThat(
                            config -> hasBoundedConnectTimeout((ConnectionConfig) config)));
            verify(builder).setConnectionManager(connectionManager);
            verify(builder).setDefaultRequestConfig(org.mockito.ArgumentMatchers.argThat(
                    config -> hasBoundedRequestTimeouts((RequestConfig) config)));
            verify(httpClient).close();
        }
    }

    private static boolean hasBoundedConnectTimeout(ConnectionConfig config) {
        return config.getConnectTimeout().toSeconds() == 10;
    }

    private static boolean hasBoundedRequestTimeouts(RequestConfig config) {
        return config.getConnectionRequestTimeout().toSeconds() == 10
                && config.getResponseTimeout().toSeconds() == 30;
    }

    @Test
    void rejectsInvalidAuthenticationBeforeAllocatingHttpClient() {
        assertThrows(IllegalArgumentException.class,
                () -> new CoreAuthentication(" ", "connector", "secret"));
    }
}
