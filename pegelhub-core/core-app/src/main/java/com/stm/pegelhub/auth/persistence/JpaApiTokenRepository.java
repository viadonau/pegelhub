package com.stm.pegelhub.auth.persistence;

import com.stm.pegelhub.auth.persistence.JpaApiToken;
import com.stm.pegelhub.contact.persistence.JpaContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@code ApiToken}s.
 */
@Repository
public interface JpaApiTokenRepository extends JpaRepository<JpaApiToken, UUID> {

    /**
     * Retrieves {@code ApiToken} by its {@code ApiToken.hashedToken} if it exists.
     * @param hashedToken hashed apiKey of {@code ApiToken}
     * @return {@code ApiToken} if it exists or {@code Optional.empty}
     */
    Optional<JpaApiToken> getByHashedToken(String hashedToken);
}
