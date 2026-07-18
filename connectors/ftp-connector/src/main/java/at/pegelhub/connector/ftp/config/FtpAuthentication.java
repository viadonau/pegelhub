package at.pegelhub.connector.ftp.config;

import at.pegelhub.lib.config.ConfigValidation;

public record FtpAuthentication(
        String username,
        String password
) {
    public FtpAuthentication {
        username = ConfigValidation.requireText(username, "ftp.server.authentication.username");
        password = ConfigValidation.requireText(password, "ftp.server.authentication.password");
    }
}
