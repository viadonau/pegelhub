package at.pegelhub.access.application;

import at.pegelhub.access.domain.AccessGrant;
import at.pegelhub.access.domain.AccessPermission;
import at.pegelhub.access.domain.AccessResourceRef;
import at.pegelhub.access.domain.AccessResourceType;
import at.pegelhub.access.persistence.AccessGrantRepository;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.timeseries.application.TimeSeriesService;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.springframework.stereotype.Service;

import java.time.Instant;

import static java.util.Objects.requireNonNull;

@Service
final class AccessAuthorizationServiceImpl implements AccessAuthorizationService {

    private final AccessGrantRepository accessGrants;
    private final TimeSeriesService timeSeries;

    AccessAuthorizationServiceImpl(AccessGrantRepository accessGrants, TimeSeriesService timeSeries) {
        this.accessGrants = requireNonNull(accessGrants);
        this.timeSeries = requireNonNull(timeSeries);
    }

    @Override
    public boolean isAllowed(
            ConnectorId connectorId,
            AccessResourceRef resource,
            AccessPermission permission,
            Instant at) {
        requireNonNull(connectorId);
        requireNonNull(resource);
        requireNonNull(permission);
        var checkedAt = at == null ? Instant.now() : at;
        return accessGrants.findByConnectorId(connectorId).stream()
                .filter(grant -> isCurrentlyValid(grant, checkedAt))
                .filter(grant -> permissionAllows(grant.permission(), permission))
                .anyMatch(grant -> resourceAllows(grant, resource));
    }

    @Override
    public boolean isAllowedForFutureTimeSeries(
            ConnectorId connectorId,
            StationId stationId,
            AccessPermission permission,
            Instant at) {
        requireNonNull(connectorId);
        requireNonNull(stationId);
        requireNonNull(permission);
        var checkedAt = at == null ? Instant.now() : at;
        return accessGrants.findByConnectorId(connectorId).stream()
                .filter(grant -> isCurrentlyValid(grant, checkedAt))
                .filter(grant -> permissionAllows(grant.permission(), permission))
                .anyMatch(grant -> grant.resource().type() == AccessResourceType.STATION
                        && grant.resource().id().equals(stationId.value())
                        && grant.includeFutureTimeSeries());
    }

    private boolean isCurrentlyValid(AccessGrant grant, Instant at) {
        return (grant.validFrom() == null || !at.isBefore(grant.validFrom()))
                && (grant.validUntil() == null || at.isBefore(grant.validUntil()));
    }

    private boolean permissionAllows(AccessPermission granted, AccessPermission requested) {
        return granted == AccessPermission.MANAGE || granted == requested;
    }

    private boolean resourceAllows(AccessGrant grant, AccessResourceRef requestedResource) {
        if (grant.resource().equals(requestedResource)) {
            return true;
        }
        if (grant.resource().type() != AccessResourceType.STATION
                || requestedResource.type() != AccessResourceType.TIME_SERIES) {
            return false;
        }
        var requestedTimeSeries = timeSeries.get(new TimeSeriesId(requestedResource.id()));
        return grant.resource().id().equals(requestedTimeSeries.stationId().value());
    }
}
