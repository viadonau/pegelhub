package at.pegelhub.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pegelhub.security")
public record PegelHubSecurityProperties(
        String issuerUri
) {
    static final String API_AUDIENCE = "pegelhub-core-api";
}
