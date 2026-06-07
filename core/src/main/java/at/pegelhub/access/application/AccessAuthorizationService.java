package at.pegelhub.access.application;

import at.pegelhub.access.domain.AccessPermission;
import at.pegelhub.access.domain.AccessResourceRef;
import at.pegelhub.connector.domain.ConnectorId;

public interface AccessAuthorizationService {

    /**
     * Returns whether the connector has the requested permission for the resource.
     * Direct grants match their exact resource, while station-scoped READ grants also cover TimeSeries
     * belonging to that Station.
     */
    boolean isAllowed(
            ConnectorId connectorId,
            AccessResourceRef resource,
            AccessPermission permission);
}
