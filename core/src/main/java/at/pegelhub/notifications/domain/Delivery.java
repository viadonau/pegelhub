package at.pegelhub.notifications.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.UUID;

/**
 * Durable content and non-secret route snapshot for one destination. Attempts count claims, not confirmed sends.
 * ACCEPTED means transport completion, not receipt; the private lease token fences only database completion.
 */
public record Delivery(
        UUID id,
        UUID requestId,
        UUID sourceId,
        UUID destinationId,
        DestinationConfig route,
        String subject,
        String body,
        State state,
        int attempts,
        Instant createdAt,
        Instant nextAttemptAt,
        Instant completedAt,
        String lastError,
        @JsonIgnore UUID leaseToken) {

    public enum State { PENDING, SENDING, ACCEPTED, FAILED, CANCELLED }
}
