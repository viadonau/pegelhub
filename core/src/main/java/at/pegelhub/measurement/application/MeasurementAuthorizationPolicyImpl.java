package at.pegelhub.measurement.application;

import at.pegelhub.access.application.AccessAuthorizationService;
import at.pegelhub.access.domain.AccessPermission;
import at.pegelhub.access.domain.AccessResourceRef;
import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.connector.domain.ConnectorStatus;
import at.pegelhub.connector.persistence.ConnectorRepository;
import at.pegelhub.security.CurrentActor;
import at.pegelhub.security.PegelHubActor;
import at.pegelhub.security.PegelHubActorType;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.timeseries.application.TimeSeriesService;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Collection;
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
    private final AccessAuthorizationService accessAuthorizationService;

    MeasurementAuthorizationPolicyImpl(
            CurrentActor currentActor,
            ConnectorRepository connectorRepository,
            TimeSeriesService timeSeriesService,
            AccessAuthorizationService accessAuthorizationService) {
        this.currentActor = requireNonNull(currentActor);
        this.connectorRepository = requireNonNull(connectorRepository);
        this.timeSeriesService = requireNonNull(timeSeriesService);
        this.accessAuthorizationService = requireNonNull(accessAuthorizationService);
    }

    @Override
    public ConnectorId requireWrite(TimeSeriesId timeSeriesId) {
        return requireWriteBatch(Set.of(timeSeriesId));
    }

    @Override
    public ConnectorId requireWriteBatch(Collection<TimeSeriesId> timeSeriesIds) {
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
        for (TimeSeriesId timeSeriesId : new LinkedHashSet<>(timeSeriesIds)) {
            TimeSeries timeSeries = timeSeriesService.get(requireNonNull(timeSeriesId));
            if (!connectorId.equals(timeSeries.sourceConnectorId())) {
                throw new AccessDeniedException(
                        "Connector is not allowed to write measurements for TimeSeries " + timeSeriesId.value()
                                + ": connector is not the source connector");
            }
            if (!accessAuthorizationService.isAllowed(
                    connectorId,
                    AccessResourceRef.timeSeries(timeSeriesId),
                    AccessPermission.WRITE)) {
                throw new AccessDeniedException(
                        "Connector is not allowed to write measurements for TimeSeries " + timeSeriesId.value()
                                + ": missing write grant");
            }
        }
        return connectorId;
    }

    @Override
    public void requireRead(TimeSeriesId timeSeriesId) {
        timeSeriesService.get(timeSeriesId);
        PegelHubActor actor = currentActor.get();
        if (actor.hasAuthority(SYSTEM_ADMIN)) {
            return;
        }
        if (!actor.hasAuthority(MEASUREMENT_READ)) {
            throw new AccessDeniedException("Actor is not allowed to read measurements");
        }
        if (actor.type() == PegelHubActorType.USER) {
            return;
        }

        Connector connector = requireActiveConnector(actor);
        if (!accessAuthorizationService.isAllowed(
                connector.id(),
                AccessResourceRef.timeSeries(timeSeriesId),
                AccessPermission.READ)) {
            throw new AccessDeniedException("Connector is not allowed to read this TimeSeries");
        }
    }

    private Connector requireActiveConnector(PegelHubActor actor) {
        if (actor.clientId() == null || actor.clientId().isBlank()) {
            throw new NotFoundException("Connector not registered");
        }
        Connector connector = connectorRepository.findByKeycloakClientId(actor.clientId())
                .orElseThrow(() -> new NotFoundException("Connector not registered"));
        if (connector.status() != ConnectorStatus.ACTIVE) {
            throw new AccessDeniedException("Connector is not active");
        }
        return connector;
    }
}
