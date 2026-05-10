package com.stm.pegelhub.auth.persistence;

import com.stm.pegelhub.shared.persistence.IdentifiableEntity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Data class for {@code ApiToken}s.
 */

@Entity
@Data
@Table(name = "ApiToken")
public class JpaApiToken extends IdentifiableEntity {

    @Column(nullable = false)
    private String hashedToken;

    @Column(nullable = false)
    private String salt;

    @Column(nullable = false)
    private boolean activated;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public JpaApiToken(UUID id, String hashedToken, String salt, boolean activated, LocalDateTime expiresAt) {
        this.id = id;
        this.hashedToken = hashedToken;
        this.salt = salt;
        this.activated = activated;
        this.expiresAt = expiresAt;
    }

    public JpaApiToken() {
    }
}
