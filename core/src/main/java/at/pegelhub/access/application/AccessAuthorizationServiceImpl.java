package at.pegelhub.access.application;

import at.pegelhub.access.domain.AccessGrant;
import at.pegelhub.access.domain.AccessPermission;
import at.pegelhub.access.domain.AccessResourceRef;
import at.pegelhub.access.domain.AccessResourceType;
import at.pegelhub.access.persistence.AccessGrantRepository;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.timeseries.application.TimeSeriesService;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Service
class AccessAuthorizationServiceImpl implements AccessAuthorizationService {

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
            AccessPermission permission) {
        requireNonNull(connectorId);
        requireNonNull(resource);
        requireNonNull(permission);
        List<AccessGrant> matchingGrants = accessGrants.findByConnectorId(connectorId).stream()
                .filter(grant -> grant.permission() == permission)
                .toList();
        Optional<TimeSeries> stationScopedTimeSeries = requestedTimeSeriesForStationScopedGrant(resource, matchingGrants);
        return matchingGrants.stream()
                .anyMatch(grant -> grantCoversResource(grant, resource, stationScopedTimeSeries));
    }

    /**
     * Resolves the requested TimeSeries only when it is needed to check station-scoped coverage.
     * Direct TimeSeries grants can be matched by ID alone, but a Station grant must compare its
     * Station ID with the requested TimeSeries' owning Station.
     */
    private Optional<TimeSeries> requestedTimeSeriesForStationScopedGrant(
            AccessResourceRef requestedResource,
            List<AccessGrant> matchingGrants) {
        if (requestedResource.type() != AccessResourceType.TIME_SERIES) {
            return Optional.empty();
        }
        boolean hasStationGrant = matchingGrants.stream()
                .anyMatch(grant -> grant.resource().type() == AccessResourceType.STATION);
        return hasStationGrant
                ? Optional.of(timeSeries.get(new TimeSeriesId(requestedResource.id())))
                : Optional.empty();
    }

    /**
     * Checks whether a grant's resource covers the requested resource.
     * Direct resource matches are exact; Station grants cover TimeSeries whose stationId matches
     * the grant's Station resource ID.
     */
    private boolean grantCoversResource(
            AccessGrant grant,
            AccessResourceRef requestedResource,
            Optional<TimeSeries> stationScopedTimeSeries) {
        if (grant.resource().equals(requestedResource)) {
            return true;
        }
        if (grant.resource().type() != AccessResourceType.STATION
                || requestedResource.type() != AccessResourceType.TIME_SERIES) {
            return false;
        }
        return stationScopedTimeSeries
                .map(timeSeries -> grant.resource().id().equals(timeSeries.stationId().value()))
                .orElse(false);
    }
}
