package at.pegelhub.access.application;

import at.pegelhub.access.domain.AccessPermission;
import at.pegelhub.access.domain.AccessResourceRef;
import at.pegelhub.connector.domain.ConnectorId;

import java.time.Instant;

public record CreateAccessGrantCommand(
        ConnectorId connectorId,
        AccessResourceRef resource,
        AccessPermission permission,
        Instant validFrom,
        Instant validUntil,
        boolean includeFutureTimeSeries
) {
}
