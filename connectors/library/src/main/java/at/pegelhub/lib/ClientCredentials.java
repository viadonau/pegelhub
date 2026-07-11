package at.pegelhub.lib;

public record ClientCredentials(String tokenUrl, String clientId, String clientSecret) {
    public ClientCredentials {
        tokenUrl = requireText(tokenUrl, "tokenUrl");
        clientId = requireText(clientId, "clientId");
        clientSecret = requireText(clientSecret, "clientSecret");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
