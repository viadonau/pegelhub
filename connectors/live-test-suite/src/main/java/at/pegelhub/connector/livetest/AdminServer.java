package at.pegelhub.connector.livetest;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

final class AdminServer implements AutoCloseable {
    private final HarnessState state;
    private final HttpServer server;

    AdminServer(HarnessState state) throws IOException {
        this.state = state;
        this.server = HttpServer.create(new InetSocketAddress("0.0.0.0", SuiteConstants.ADMIN_PORT), 0);
        this.server.createContext("/health", exchange -> HttpSupport.respondText(exchange, 200, "OK\n"));
        this.server.createContext("/state", exchange -> HttpSupport.respondText(exchange, 200, Verifier.summary(state) + "\n"));
        this.server.createContext("/verify", exchange -> {
            String scenario = HttpSupport.query(exchange.getRequestURI().getRawQuery()).getOrDefault("scenario", "all");
            VerificationResult result = Verifier.verify(state, Scenario.parse(scenario));
            HttpSupport.respondText(exchange, result.success() ? 200 : 500, result.message() + "\n");
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
