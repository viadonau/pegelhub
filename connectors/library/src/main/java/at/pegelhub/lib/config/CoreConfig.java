package at.pegelhub.lib.config;

import java.net.URL;
import java.util.Objects;

public record CoreConfig(URL baseUrl) {
    public CoreConfig {
        Objects.requireNonNull(baseUrl, "baseUrl");
    }
}
