package at.pegelhub.auth.persistence;

import at.pegelhub.auth.domain.ApiToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for ApiTokens.
 */

public interface ApiTokenRepository {

    /**
     * Adds provided {@code ApiToken}.
     * @param apiToken {@code ApiToken} to add.
     * @return added {@code ApiToken}
     */
    ApiToken save(ApiToken apiToken);

    /**
     * Retrieves {@code ApiToken} by its {@code ApiToken.id} if it exists.
     * @param uuid {@code UUID} of the {@code ApiToken}
     * @return {@code ApiToken} if it exists or {@code Optional.empty}
     */
    Optional<ApiToken> getById(UUID uuid);

    /**
     * Retrieves {@code ApiToken} by its {@code ApiToken.hashedToken} if it exists.
     * @param hashedToken hashed apiKey of {@code ApiToken}
     * @return {@code ApiToken} if it exists or {@code Optional.empty}
     */
    Optional<ApiToken> getByHashedToken(String hashedToken);

    /**
     * Attempts to delete {@code ApiToken} by its {@code ApiToken.id}
     * @param uuid {@code UUID} of the {@code ApiToken}
     */
    void delete(UUID uuid);

    /**
     * Updates provided {@code ApiToken}.
     * @param apiToken {@code ApiToken} to update.
     * @return updated {@code ApiToken}
     */
    ApiToken update(ApiToken apiToken);

    /**
     * Retrieves all {@code ApiToken}s.
     * @return All available {@code ApiToken}s.
     */
    List<ApiToken> getAll();


}

