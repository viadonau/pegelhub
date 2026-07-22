package at.pegelhub.access.application;

import at.pegelhub.access.domain.AccessGrant;
import at.pegelhub.access.domain.AccessPermission;
import at.pegelhub.access.domain.AccessResourceRef;
import at.pegelhub.access.domain.AccessResourceType;
import at.pegelhub.access.persistence.AccessGrantRepository;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measuringpoint.application.MeasuringPointService;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.timeseries.application.TimeSeriesService;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Service
class AccessAuthorizationServiceImpl implements AccessAuthorizationService {

    private final AccessGrantRepository accessGrants;
    private final TimeSeriesService timeSeries;
    private final MeasuringPointService measuringPoints;

    AccessAuthorizationServiceImpl(
            AccessGrantRepository accessGrants,
            TimeSeriesService timeSeries,
            MeasuringPointService measuringPoints) {
        this.accessGrants = requireNonNull(accessGrants);
        this.timeSeries = requireNonNull(timeSeries);
        this.measuringPoints = requireNonNull(measuringPoints);
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
        Optional<StationId> stationScopedTimeSeriesStation = requestedStationForStationScopedGrant(
                resource,
                matchingGrants);
        return matchingGrants.stream()
                .anyMatch(grant -> grantCoversResource(grant, resource, stationScopedTimeSeriesStation));
    }

    /**
     * Resolves the requested TimeSeries only when it is needed to check station-scoped coverage.
     * Direct TimeSeries grants can be matched by ID alone, but a Station grant must compare its
     * Station ID with the Station containing the requested TimeSeries' MeasuringPoint.
     */
    private Optional<StationId> requestedStationForStationScopedGrant(
            AccessResourceRef requestedResource,
            List<AccessGrant> matchingGrants) {
        if (requestedResource.type() != AccessResourceType.TIME_SERIES) {
            return Optional.empty();
        }
        boolean hasStationGrant = matchingGrants.stream()
                .anyMatch(grant -> grant.resource().type() == AccessResourceType.STATION);
        if (!hasStationGrant) {
            return Optional.empty();
        }
        var requestedTimeSeries = timeSeries.get(new TimeSeriesId(requestedResource.id()));
        return Optional.of(measuringPoints.get(requestedTimeSeries.measuringPointId()).stationId());
    }

    /**
     * Checks whether a grant's resource covers the requested resource.
     * Direct resource matches are exact; Station grants cover TimeSeries whose MeasuringPoint is
     * contained by the granted Station.
     */
    private boolean grantCoversResource(
            AccessGrant grant,
            AccessResourceRef requestedResource,
            Optional<StationId> stationScopedTimeSeriesStation) {
        if (grant.resource().equals(requestedResource)) {
            return true;
        }
        if (grant.resource().type() != AccessResourceType.STATION
                || requestedResource.type() != AccessResourceType.TIME_SERIES) {
            return false;
        }
        return stationScopedTimeSeriesStation
                .map(StationId::value)
                .map(grant.resource().id()::equals)
                .orElse(false);
    }
}
