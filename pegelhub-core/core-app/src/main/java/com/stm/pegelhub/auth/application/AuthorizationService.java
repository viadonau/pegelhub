package com.stm.pegelhub.auth.application;

import com.stm.pegelhub.shared.error.UnauthorizedException;

import java.util.UUID;

/**
 * Interface for the authorization service.
 */
public interface AuthorizationService {
    /**
     * Throws UnauthorizedException if provided {@code apiKey} is invalid.
     *
     * @param apiKey Key to access data.
     * @return the id of the authorized user.
     * @throws UnauthorizedException If the {@code apiKey} is invalid.
     */

    UUID authorize(String apiKey);

}
