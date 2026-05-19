package at.pegelhub.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pegelhub.security")
public record PegelHubSecurityProperties(
        String issuerUri,
        String audience,
        String apiClientId
) {
    private static final String DEFAULT_API_AUDIENCE = "pegelhub-core-api";

    public PegelHubSecurityProperties {
        audience = hasText(audience) ? audience : DEFAULT_API_AUDIENCE;
        apiClientId = hasText(apiClientId) ? apiClientId : audience;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
