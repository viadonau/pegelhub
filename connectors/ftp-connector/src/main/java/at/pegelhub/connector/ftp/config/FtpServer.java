package at.pegelhub.connector.ftp.config;

import at.pegelhub.lib.config.ConfigValidation;

import java.util.Objects;

public record FtpServer(
        String host,
        int port,
        FtpAuthentication authentication
) {
    public FtpServer {
        host = ConfigValidation.requireText(host, "ftp.server.host");
        port = ConfigValidation.requireTcpPort(port, "ftp.server.port");
        Objects.requireNonNull(authentication, "ftp.server.authentication");
    }
}
