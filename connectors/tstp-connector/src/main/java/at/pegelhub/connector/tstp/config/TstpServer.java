package at.pegelhub.connector.tstp.config;

import at.pegelhub.lib.config.ConfigValidation;

public record TstpServer(
        String host,
        int port
) {
    public TstpServer {
        host = ConfigValidation.requireText(host, "tstp.server.host");
        port = ConfigValidation.requireTcpPort(port, "tstp.server.port");
    }
}
