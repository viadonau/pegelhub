package at.pegelhub.lib;

import java.net.URL;
import java.util.Objects;

public record CoreConnection(URL baseUrl, ClientCredentials credentials) {
    public CoreConnection {
        Objects.requireNonNull(baseUrl, "baseUrl");
        Objects.requireNonNull(credentials, "credentials");
    }
}
