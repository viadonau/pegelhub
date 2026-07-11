package at.pegelhub.lib.config;

import at.pegelhub.lib.ClientCredentials;

public record KeycloakConfig(String tokenUrl, String clientId, String clientSecret) {
    public ClientCredentials credentials() {
        return new ClientCredentials(tokenUrl, clientId, clientSecret);
    }
}
