package at.pegelhub.lib.config;

import at.pegelhub.lib.ClientCredentials;

public record KeycloakConfig(String tokenUrl, String clientId, String clientSecret) {
    public KeycloakConfig {
        tokenUrl = ConfigValidation.requireText(tokenUrl, "keycloak.tokenUrl");
        clientId = ConfigValidation.requireText(clientId, "keycloak.clientId");
        clientSecret = ConfigValidation.requireText(clientSecret, "keycloak.clientSecret");
    }

    public ClientCredentials credentials() {
        return new ClientCredentials(tokenUrl, clientId, clientSecret);
    }
}
