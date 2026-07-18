package at.pegelhub.connector.iec.config;

import at.pegelhub.lib.config.ConfigValidation;

public record IecServer(
        String host,
        int port,
        int commonAddress
) {
    public IecServer {
        host = ConfigValidation.requireText(host, "iec.server.host");
        port = ConfigValidation.requireTcpPort(port, "iec.server.port");
        commonAddress = ConfigValidation.requirePositive(commonAddress, "iec.server.commonAddress");
    }
}
