package at.pegelhub.access.api;

import at.pegelhub.access.domain.AccessPermission;
import at.pegelhub.access.domain.AccessResourceType;

import java.time.Instant;
import java.util.UUID;

public record AccessGrantResponse(
        UUID id,
        UUID connectorId,
        AccessResourceType resourceType,
        UUID resourceId,
        AccessPermission permission,
        Instant validFrom,
        Instant validUntil,
        boolean includeFutureTimeSeries
) {
}
