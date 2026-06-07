package at.pegelhub.measurement.application;

import at.pegelhub.access.application.AccessAuthorizationService;
import at.pegelhub.access.domain.AccessPermission;
import at.pegelhub.access.domain.AccessResourceRef;
import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.connector.domain.ConnectorStatus;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.measurement.domain.WriteMeasurement;
import at.pegelhub.measurement.domain.WriteMeasurements;
import at.pegelhub.security.CurrentActor;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.measurement.persistence.MeasurementRepository;
import at.pegelhub.connector.persistence.ConnectorRepository;
import at.pegelhub.timeseries.application.TimeSeriesService;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Default implementation for {@code MeasurementService}.
 */
@Service
public final class MeasurementServiceImpl implements MeasurementService {

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
        if (connector.getId() == null) {
            throw new NotFoundException("Connector not registered");
        }
        if (connector.getStatus() != ConnectorStatus.ACTIVE) {
            throw new AccessDeniedException("Connector is not active");
        }
        ConnectorId connectorId = new ConnectorId(connector.getId());
        Instant receivedAt = Instant.now();
        List<Measurement> measurements = new ArrayList<>(writeMeasurements.measurements().size());
        for (WriteMeasurement measurement : writeMeasurements.measurements()) {
            timeSeriesService.get(measurement.timeSeriesId());
            if (!accessAuthorizationService.isAllowed(
                    connectorId,
                    AccessResourceRef.timeSeries(measurement.timeSeriesId()),
                    AccessPermission.WRITE,
                    receivedAt)) {
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

    /**
     * @param range in which the returned values reside.
     * @return all saved measurements in the specified range
     */
    @Override
    public List<Measurement> getByRange(String range) {
        return measurementRepository.getByRange(range);
    }

    @Override
    public List<Measurement> getByTimeSeriesAndRange(TimeSeriesId timeSeriesId, String range) {
        timeSeriesService.get(timeSeriesId);
        return measurementRepository.getByTimeSeriesIdAndRange(timeSeriesId, range);
    }

    @Override
    public Measurement getLatestByTimeSeries(TimeSeriesId timeSeriesId) {
        timeSeriesService.get(timeSeriesId);
        return measurementRepository.getLatestByTimeSeriesId(timeSeriesId);
    }

    @Override
    public Measurement getAverageByTimeSeriesAndRange(TimeSeriesId timeSeriesId, String range) {
        timeSeriesService.get(timeSeriesId);
        return measurementRepository.getAverageByTimeSeriesIdAndRange(timeSeriesId, range);
    }

    /**
     *
      * @param uuid of the measurement.
     * @return gets the last {@link Measurement} with the specified {@link UUID}
     */
    @Override
    public Measurement getLastData(UUID uuid) {
        return getLatestByTimeSeries(new TimeSeriesId(uuid));
    }

    public Instant getSystemTime()
    {
        return measurementRepository.getSystemTime();
    }
}
