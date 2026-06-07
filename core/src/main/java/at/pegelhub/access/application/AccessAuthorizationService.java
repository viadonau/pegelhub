package at.pegelhub.access.application;

import at.pegelhub.access.domain.AccessPermission;
import at.pegelhub.access.domain.AccessResourceRef;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.station.domain.StationId;

import java.time.Instant;

public interface AccessAuthorizationService {

    boolean isAllowed(
            ConnectorId connectorId,
            AccessResourceRef resource,
            AccessPermission permission,
            Instant at);

    boolean isAllowedForFutureTimeSeries(
            ConnectorId connectorId,
            StationId stationId,
            AccessPermission permission,
            Instant at);
}
