package at.pegelhub.auth.api;

/**
 * DTO for API tokens. When received, apiKey. When sent, hashed apiKey (unless when created initially/refreshed).
 */
public record ApiTokenDto(String apiKey) {
    public ApiTokenDto {
        if (apiKey.length() != 64) {
            throw new IllegalArgumentException("your arguments are invalid");
        }
    }
}
