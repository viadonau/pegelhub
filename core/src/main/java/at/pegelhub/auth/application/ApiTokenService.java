package at.pegelhub.auth.application;

import java.util.List;
import java.util.UUID;

/**
 * Service-Class for ApiTokens.
 */
public interface ApiTokenService {

    /**
     * Creates a new {@code ApiToken}.
     * @return unhashed apiKey of created {@code ApiToken}
     */
    String createToken();

    /**
     * Refreshes an existing and currently valid {@code ApiToken}
     * @param apiKey unhashed apiKey of the to be refreshed {@code ApiToken}
     * @return unhashed apiKey of refreshed {@code ApiToken}
     */
    String refreshToken(String apiKey, UUID connectorUUID);

    /**
     * Invalidates an existing {@code ApiToken}
     * @param apiKey nhashed apiKey of the to be invalidated {@code ApiToken}
     */
    void invalidateToken(String apiKey, UUID uuid);

    /**
     * Retrieves the {@code UUID}s of all available {@code ApiToken}s.
     * @return {@code UUID}s of all available {@code ApiToken}s.
     */
    List<UUID> getTokens();

    /**
     * Activates an existing {@code ApiToken}.
     * @param uuid {@code UUID} of the to be activated {@code ApiToken}
     */
    void activateToken(UUID uuid);
}
