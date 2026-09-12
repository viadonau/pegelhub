package at.pegelhub.measurement.application;

import at.pegelhub.access.application.ConnectorReadAccessService;
import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.connector.persistence.ConnectorRepository;
import at.pegelhub.security.CurrentActor;
import at.pegelhub.security.PegelHubActor;
import at.pegelhub.security.PegelHubActorType;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.shared.metadata.MetadataStatus;
import at.pegelhub.timeseries.application.TimeSeriesService;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashSet;

import static at.pegelhub.security.PegelHubAuthority.MEASUREMENT_READ;
import static at.pegelhub.security.PegelHubAuthority.MEASUREMENT_WRITE;
import static at.pegelhub.security.PegelHubAuthority.SYSTEM_ADMIN;
import static java.util.Objects.requireNonNull;

@Service
class MeasurementAuthorizationPolicyImpl implements MeasurementAuthorizationPolicy {

    private final CurrentActor currentActor;
    private final ConnectorRepository connectorRepository;
    private final TimeSeriesService timeSeriesService;
    private final ConnectorReadAccessService readAccess;

    MeasurementAuthorizationPolicyImpl(
            CurrentActor currentActor,
            ConnectorRepository connectorRepository,
            TimeSeriesService timeSeriesService,
            ConnectorReadAccessService readAccess) {
        this.currentActor = requireNonNull(currentActor);
        this.connectorRepository = requireNonNull(connectorRepository);
        this.timeSeriesService = requireNonNull(timeSeriesService);
        this.readAccess = requireNonNull(readAccess);
    }

    @Override
    public ConnectorId requireWriter() {
        var actor = currentActor.get();
        if (!actor.hasAuthority(MEASUREMENT_WRITE)) {
            throw new AccessDeniedException("Actor is not allowed to write measurements");
        }
        if (actor.type() != PegelHubActorType.CLIENT) {
            throw new AccessDeniedException("Only connector clients may write measurements");
        }

        return requireActiveConnector(actor).id();
    }

    @Override
    public void requireWrite(ConnectorId connectorId, MeasurementWriteTarget target) {
        requireNonNull(connectorId);
        requireNonNull(target);
        requireActiveTarget(target);

        var series = target.timeSeries();
        if (!connectorId.equals(series.sourceConnectorId())) {
            throw new AccessDeniedException(
                    "Connector is not allowed to write measurements for TimeSeries "
                            + series.id().value()
                            + ": connector is not the source connector");
        }
    }

    private void requireActiveTarget(MeasurementWriteTarget target) {
        if (target.timeSeries().status() != MetadataStatus.ACTIVE) {
            throw new AccessDeniedException("TimeSeries is not active");
        }
        if (target.measuringPoint().status() != MetadataStatus.ACTIVE) {
            throw new AccessDeniedException("Measuring point is not active");
        }
        if (target.station().status() != MetadataStatus.ACTIVE) {
            throw new AccessDeniedException("Station is not active");
        }
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
        Connector connector = connectorRepository
                .findByKeycloakClientId(actor.clientId())
                .orElseThrow(() -> new NotFoundException("Connector not registered"));
        if (connector.status() != MetadataStatus.ACTIVE) {
            throw new AccessDeniedException("Connector is not active");
        }
        return connector;
    }

}
