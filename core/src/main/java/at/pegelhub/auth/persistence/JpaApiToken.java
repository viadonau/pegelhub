package at.pegelhub.auth.persistence;

import at.pegelhub.shared.persistence.IdentifiableEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Data class for {@code ApiToken}s.
 */

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
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
