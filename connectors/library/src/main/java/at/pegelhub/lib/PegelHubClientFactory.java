package at.pegelhub.lib;

import at.pegelhub.lib.internal.HttpPegelHubClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import java.net.URL;

/**
 * Creates PegelHub Core clients for connector runtime code.
 */
public final class PegelHubClientFactory {
    private PegelHubClientFactory() {
    }

    public static PegelHubClient create(CoreConnection connection) {
        return create(connection.baseUrl(), connection.credentials());
    }

    public static PegelHubClient create(URL baseUrl, ClientCredentials credentials) {
        CloseableHttpClient client = HttpClients.createDefault();
        return new HttpPegelHubClient(client, baseUrl, credentials);
    }
}
