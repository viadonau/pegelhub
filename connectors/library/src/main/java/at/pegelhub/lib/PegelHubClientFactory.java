package at.pegelhub.lib;

import at.pegelhub.lib.internal.HttpPegelHubClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;

@FunctionalInterface
public interface PegelHubClientFactory {
    PegelHubClient create(CoreConnection connection);

    static PegelHubClientFactory http() {
        return connection -> new HttpPegelHubClient(
                HttpClients.createDefault(),
                connection.baseUrl(),
                connection.credentials());
    }
}
