package at.pegelhub.lib;

import at.pegelhub.lib.config.ConfigValidation;

public record ClientCredentials(String tokenUrl, String clientId, String clientSecret) {
    public ClientCredentials {
        tokenUrl = ConfigValidation.requireText(tokenUrl, "tokenUrl");
        clientId = ConfigValidation.requireText(clientId, "clientId");
        clientSecret = ConfigValidation.requireText(clientSecret, "clientSecret");
    }
}
