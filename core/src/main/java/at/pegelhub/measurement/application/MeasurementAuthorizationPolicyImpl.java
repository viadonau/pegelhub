package at.pegelhub.measurement.application;

import at.pegelhub.access.application.ConnectorReadAccessService;
import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.connector.persistence.ConnectorRepository;
import at.pegelhub.measuringpoint.application.MeasuringPointService;
import at.pegelhub.station.application.StationService;
import at.pegelhub.security.CurrentActor;
import at.pegelhub.security.PegelHubActor;
import at.pegelhub.security.PegelHubActorType;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.shared.metadata.MetadataStatus;
import at.pegelhub.timeseries.application.TimeSeriesService;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import static at.pegelhub.security.PegelHubAuthority.MEASUREMENT_READ;
import static at.pegelhub.security.PegelHubAuthority.MEASUREMENT_WRITE;
import static at.pegelhub.security.PegelHubAuthority.SYSTEM_ADMIN;
import static java.util.Objects.requireNonNull;

@Service
class MeasurementAuthorizationPolicyImpl implements MeasurementAuthorizationPolicy {

    private final CurrentActor currentActor;
    private final ConnectorRepository connectorRepository;
    private final TimeSeriesService timeSeriesService;
    private final MeasuringPointService measuringPoints;
    private final StationService stations;
    private final ConnectorReadAccessService readAccess;

    MeasurementAuthorizationPolicyImpl(
            CurrentActor currentActor,
            ConnectorRepository connectorRepository,
            TimeSeriesService timeSeriesService,
            MeasuringPointService measuringPoints,
            StationService stations,
            ConnectorReadAccessService readAccess) {
        this.currentActor = requireNonNull(currentActor);
        this.connectorRepository = requireNonNull(connectorRepository);
        this.timeSeriesService = requireNonNull(timeSeriesService);
        this.measuringPoints = requireNonNull(measuringPoints);
        this.stations = requireNonNull(stations);
        this.readAccess = requireNonNull(readAccess);
    }

    @Override
    public MeasurementWriteAuthorization requireWrite(TimeSeriesId timeSeriesId) {
        return requireWriteBatch(Set.of(timeSeriesId));
    }

    @Override
    public MeasurementWriteAuthorization requireWriteBatch(Collection<TimeSeriesId> timeSeriesIds) {
        if (timeSeriesIds == null || timeSeriesIds.isEmpty()) {
            throw new IllegalArgumentException("timeSeriesIds must not be empty");
        }

        PegelHubActor actor = currentActor.get();
        if (!actor.hasAuthority(MEASUREMENT_WRITE)) {
            throw new AccessDeniedException("Actor is not allowed to write measurements");
        }
        if (actor.type() != PegelHubActorType.CLIENT) {
            throw new AccessDeniedException("Only connector clients may write measurements");
        }

        Connector connector = requireActiveConnector(actor);
        ConnectorId connectorId = connector.id();
        var normalization = new LinkedHashMap<TimeSeriesId, MeasurementWriteAuthorization.Normalization>();
        for (TimeSeriesId timeSeriesId : new LinkedHashSet<>(timeSeriesIds)) {
            TimeSeries timeSeries = timeSeriesService.get(requireNonNull(timeSeriesId));
            var measuringPoint = requireActiveHierarchy(timeSeries);
            if (!connectorId.equals(timeSeries.sourceConnectorId())) {
                throw new AccessDeniedException(
                        "Connector is not allowed to write measurements for TimeSeries " + timeSeriesId.value()
                                + ": connector is not the source connector");
            }
            normalization.put(timeSeriesId, new MeasurementWriteAuthorization.Normalization(
                    timeSeries.sourceRepresentation(), measuringPoint.gaugeZeroElevationMAboveAdria()));
        }
        return new MeasurementWriteAuthorization(connectorId, normalization);
    }

    @Override
    public void requireRead(TimeSeriesId timeSeriesId) {
        requireNonNull(timeSeriesId);
        PegelHubActor actor = currentActor.get();
        if (actor.type() == PegelHubActorType.USER && actor.hasAuthority(SYSTEM_ADMIN)) {
            timeSeriesService.get(timeSeriesId);
            return;
        }
        if (!actor.hasAuthority(MEASUREMENT_READ)) {
            throw new AccessDeniedException("Actor is not allowed to read measurements");
        }
        if (actor.type() == PegelHubActorType.USER) {
            timeSeriesService.get(timeSeriesId);
            return;
        }

        Connector connector = requireActiveConnector(actor);
        if (!readAccess.allows(connector.id(), timeSeriesId)) {
            throw new AccessDeniedException("Connector is not allowed to read this TimeSeries");
        }
    }

    @Override
    public void requireReadBatch(Collection<TimeSeriesId> timeSeriesIds) {
        if (timeSeriesIds == null || timeSeriesIds.isEmpty()) {
            throw new IllegalArgumentException("timeSeriesIds must not be empty");
        }
        PegelHubActor actor = currentActor.get();
        if (actor.type() == PegelHubActorType.USER && actor.hasAuthority(SYSTEM_ADMIN)) {
            return;
        }
        if (!actor.hasAuthority(MEASUREMENT_READ)) {
            throw new AccessDeniedException("Actor is not allowed to read measurements");
        }
        if (actor.type() == PegelHubActorType.USER) {
            return;
        }
        for (TimeSeriesId timeSeriesId : new LinkedHashSet<>(timeSeriesIds)) {
            requireRead(requireNonNull(timeSeriesId));
        }
    }

    private Connector requireActiveConnector(PegelHubActor actor) {
        if (actor.clientId() == null || actor.clientId().isBlank()) {
            throw new NotFoundException("Connector not registered");
        }
        Connector connector = connectorRepository.findByKeycloakClientId(actor.clientId())
                .orElseThrow(() -> new NotFoundException("Connector not registered"));
        if (connector.status() != MetadataStatus.ACTIVE) {
            throw new AccessDeniedException("Connector is not active");
        }
        return connector;
    }

    private at.pegelhub.measuringpoint.domain.MeasuringPoint requireActiveHierarchy(TimeSeries series) {
        if (series.status() != MetadataStatus.ACTIVE) {
            throw new AccessDeniedException("TimeSeries is not active");
        }
        var point = measuringPoints.get(series.measuringPointId());
        if (point.status() != MetadataStatus.ACTIVE) {
            throw new AccessDeniedException("Measuring point is not active");
        }
        if (stations.get(point.stationId()).status() != MetadataStatus.ACTIVE) {
            throw new AccessDeniedException("Station is not active");
        }
        return point;
    }
}
