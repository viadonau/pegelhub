package at.pegelhub.connector.livetest;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Instant;

final class FakeKeycloakServer implements AutoCloseable {
    private final String name;
    private final HarnessState state;
    private final HttpServer server;

    FakeKeycloakServer(String name, int port, HarnessState state) throws IOException {
        this.name = name;
        this.state = state;
        this.server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        this.server.createContext("/", exchange -> {
            if (!"POST".equals(exchange.getRequestMethod())) {
                HttpSupport.respond(exchange, 405, "{\"error\":\"method_not_allowed\"}");
                return;
            }
            var form = HttpSupport.query(HttpSupport.readBody(exchange));
            state.tokenRequests.add(new TokenRequest(name, form.getOrDefault("client_id", ""), Instant.now()));
            HttpSupport.respond(exchange, 200, "{\"access_token\":\"" + SuiteConstants.TOKEN + "\",\"expires_in\":300}");
        });
    }

    void start() {
        server.start();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
