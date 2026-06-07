package at.pegelhub.measurement.application;

import at.pegelhub.access.application.AccessAuthorizationService;
import at.pegelhub.access.domain.AccessPermission;
import at.pegelhub.access.domain.AccessResourceRef;
import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.connector.domain.ConnectorStatus;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.measurement.domain.MeasurementAverage;
import at.pegelhub.measurement.domain.WriteMeasurement;
import at.pegelhub.measurement.domain.WriteMeasurements;
import at.pegelhub.security.CurrentActor;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.measurement.persistence.MeasurementRepository;
import at.pegelhub.connector.persistence.ConnectorRepository;
import at.pegelhub.security.PegelHubActor;
import at.pegelhub.timeseries.application.TimeSeriesService;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static at.pegelhub.security.PegelHubAuthority.SYSTEM_ADMIN;

/**
 * Default implementation for {@code MeasurementService}.
 */
@Service
public class MeasurementServiceImpl implements MeasurementService {

    private final ConnectorRepository connectorRepository;
    private final MeasurementRepository measurementRepository;
    private final CurrentActor currentActor;
    private final TimeSeriesService timeSeriesService;
    private final AccessAuthorizationService accessAuthorizationService;

    public MeasurementServiceImpl(
            ConnectorRepository connectorRepository,
            MeasurementRepository measurementRepository,
            CurrentActor currentActor,
            TimeSeriesService timeSeriesService,
            AccessAuthorizationService accessAuthorizationService) {
        this.connectorRepository = requireNonNull(connectorRepository);
        this.measurementRepository = requireNonNull(measurementRepository);
        this.currentActor = requireNonNull(currentActor);
        this.timeSeriesService = requireNonNull(timeSeriesService);
        this.accessAuthorizationService = requireNonNull(accessAuthorizationService);
    }

    /**
     * processes the measurements to be saved to the time series database
     * @param writeMeasurements to save.
     */
    @Override
    public void writeMeasurements(WriteMeasurements writeMeasurements) {
        Connector connector = connectorRepository.findByKeycloakClientId(currentActor.get().clientId())
                .orElseThrow(() -> new NotFoundException("Connector not registered"));
        if (connector.status() != ConnectorStatus.ACTIVE) {
            throw new AccessDeniedException("Connector is not active");
        }
        ConnectorId connectorId = connector.id();
        Instant receivedAt = Instant.now();
        List<Measurement> measurements = new ArrayList<>(writeMeasurements.measurements().size());
        for (WriteMeasurement measurement : writeMeasurements.measurements()) {
            TimeSeries timeSeries = timeSeriesService.get(measurement.timeSeriesId());
            if (timeSeries.sourceConnectorId() != null && !timeSeries.sourceConnectorId().equals(connectorId)) {
                throw new AccessDeniedException("Connector is not the source connector for this TimeSeries");
            }
            if (!accessAuthorizationService.isAllowed(
                    connectorId,
                    AccessResourceRef.timeSeries(measurement.timeSeriesId()),
                    AccessPermission.WRITE)) {
                throw new AccessDeniedException("Connector is not allowed to write this TimeSeries");
            }
            measurements.add(new Measurement(
                    measurement.timeSeriesId(),
                    measurement.observedAt(),
                    receivedAt,
                    measurement.value(),
                    connectorId));
        }
        measurementRepository.storeMeasurements(measurements);
    }

    @Override
    public List<Measurement> getByTimeSeriesAndRange(TimeSeriesId timeSeriesId, String range) {
        ensureReadAllowed(timeSeriesId);
        return measurementRepository.getByTimeSeriesIdAndRange(timeSeriesId, range);
    }

    @Override
    public Measurement getLatestByTimeSeries(TimeSeriesId timeSeriesId) {
        ensureReadAllowed(timeSeriesId);
        return measurementRepository.getLatestByTimeSeriesId(timeSeriesId);
    }

    @Override
    public MeasurementAverage getAverageByTimeSeriesAndRange(TimeSeriesId timeSeriesId, String range) {
        ensureReadAllowed(timeSeriesId);
        return measurementRepository.getAverageByTimeSeriesIdAndRange(timeSeriesId, range);
    }

    private void ensureReadAllowed(TimeSeriesId timeSeriesId) {
        timeSeriesService.get(timeSeriesId);
        PegelHubActor actor = currentActor.get();
        if (actor.hasAuthority(SYSTEM_ADMIN)) {
            return;
        }
        Connector connector = connectorRepository.findByKeycloakClientId(actor.clientId())
                .orElseThrow(() -> new NotFoundException("Connector not registered"));
        if (connector.status() != ConnectorStatus.ACTIVE) {
            throw new AccessDeniedException("Connector is not active");
        }
        if (!accessAuthorizationService.isAllowed(
                connector.id(),
                AccessResourceRef.timeSeries(timeSeriesId),
                AccessPermission.READ)) {
            throw new AccessDeniedException("Connector is not allowed to read this TimeSeries");
        }
    }

    public Instant getSystemTime()
    {
        return measurementRepository.getSystemTime();
    }
}
