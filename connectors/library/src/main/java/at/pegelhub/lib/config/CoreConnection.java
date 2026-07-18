package at.pegelhub.lib.config;

import java.net.URL;
import java.util.Objects;

public record CoreConnection(
        URL baseUrl,
        CoreAuthentication authentication
) {
    public CoreConnection {
        Objects.requireNonNull(baseUrl, "baseUrl");
        Objects.requireNonNull(authentication, "authentication");
    }
}
