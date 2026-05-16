package at.pegelhub.auth.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;


/**
 * Represents an API token with a hashed value, salt, activation status, and expiration time.
 */
@Getter
@Setter
public final class ApiToken {
    private final UUID id;
    private String hashedToken;
    private String salt;
    private boolean activated;
    private LocalDateTime expiresAt;

    public ApiToken() {
        this.id = null;
    }

    public ApiToken(UUID id, String hashedToken, String salt, boolean activated, LocalDateTime expiresAt) {
        this.id = id;
        this.hashedToken = hashedToken;
        this.salt = salt;
        this.activated = activated;
        this.expiresAt = expiresAt;
    }

    public ApiToken withId(UUID uuid) {
        return new ApiToken(uuid, hashedToken, salt, activated, expiresAt);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ApiToken) obj;
        return activated == that.activated &&
                Objects.equals(id, that.id) &&
                Objects.equals(hashedToken, that.hashedToken) &&
                Objects.equals(salt, that.salt) &&
                Objects.equals(expiresAt, that.expiresAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, hashedToken, salt, activated, expiresAt);
    }

    public String toString() {
        return hashedToken;
    }
}
