package at.pegelhub.access.domain;

import at.pegelhub.connector.domain.ConnectorId;

import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record AccessGrant(
        AccessGrantId id,
        ConnectorId connectorId,
        AccessResourceRef resource,
        AccessPermission permission,
        Instant validFrom,
        Instant validUntil,
        boolean includeFutureTimeSeries
) {

    public AccessGrant {
        requireNonNull(id);
        requireNonNull(connectorId);
        requireNonNull(resource);
        requireNonNull(permission);
        if (validFrom != null && validUntil != null && !validUntil.isAfter(validFrom)) {
            throw new IllegalArgumentException("Grant validUntil must be after validFrom");
        }
        if (includeFutureTimeSeries && resource.type() != AccessResourceType.STATION) {
            throw new IllegalArgumentException("Only station grants can include future time series");
        }
    }

    public static AccessGrant create(
            ConnectorId connectorId,
            AccessResourceRef resource,
            AccessPermission permission,
            Instant validFrom,
            Instant validUntil,
            boolean includeFutureTimeSeries) {
        return new AccessGrant(
                new AccessGrantId(UUID.randomUUID()),
                connectorId,
                resource,
                permission,
                validFrom,
                validUntil,
                includeFutureTimeSeries);
    }
}
