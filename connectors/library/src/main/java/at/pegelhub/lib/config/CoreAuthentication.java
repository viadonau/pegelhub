package at.pegelhub.lib.config;

public record CoreAuthentication(
        String tokenUrl,
        String clientId,
        String clientSecret
) {
    public CoreAuthentication {
        tokenUrl = ConfigValidation.requireText(tokenUrl, "authentication.tokenUrl");
        clientId = ConfigValidation.requireText(clientId, "authentication.clientId");
        clientSecret = ConfigValidation.requireText(clientSecret, "authentication.clientSecret");
    }
}
