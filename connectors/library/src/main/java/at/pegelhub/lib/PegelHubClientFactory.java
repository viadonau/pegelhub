package at.pegelhub.lib;

import at.pegelhub.lib.config.CoreConnection;
import at.pegelhub.lib.internal.HttpPegelHubClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;

@FunctionalInterface
public interface PegelHubClientFactory {
    PegelHubClient create(CoreConnection connection);

    static PegelHubClientFactory http() {
        return http(CoreClientOptions.connectorDefaults());
    }

    static PegelHubClientFactory http(CoreClientOptions options) {
        return connection -> {
            var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                    .setDefaultConnectionConfig(ConnectionConfig.custom()
                            .setConnectTimeout(Timeout.ofMilliseconds(options.connectTimeout().toMillis()))
                            .build())
                    .build();
            var requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(Timeout.ofMilliseconds(options.connectTimeout().toMillis()))
                    .setResponseTimeout(Timeout.ofMilliseconds(options.responseTimeout().toMillis()))
                    .build();
            return new HttpPegelHubClient(
                    HttpClients.custom()
                            .disableAutomaticRetries()
                            .setConnectionManager(connectionManager)
                            .setDefaultRequestConfig(requestConfig)
                            .build(),
                    connection.baseUrl(),
                    connection.authentication(), options);
        };
    }
}
