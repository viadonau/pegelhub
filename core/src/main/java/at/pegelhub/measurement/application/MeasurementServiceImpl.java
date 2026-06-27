package at.pegelhub.measurement.application;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.measurement.domain.WriteMeasurement;
import at.pegelhub.measurement.domain.WriteMeasurements;
import at.pegelhub.measurement.persistence.MeasurementRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Default implementation for {@code MeasurementService}.
 */
@Service
public class MeasurementServiceImpl implements MeasurementService {

    private final MeasurementRepository measurementRepository;
    private final MeasurementAuthorizationPolicy authorizationPolicy;
    private final Clock clock;

    public MeasurementServiceImpl(
            MeasurementRepository measurementRepository,
            MeasurementAuthorizationPolicy authorizationPolicy,
            Clock clock) {
        this.measurementRepository = requireNonNull(measurementRepository);
        this.authorizationPolicy = requireNonNull(authorizationPolicy);
        this.clock = requireNonNull(clock);
    }

    /**
     * processes the measurements to be saved to the time series database
     * @param writeMeasurements to save.
     */
    @Override
    public void writeMeasurements(WriteMeasurements writeMeasurements) {
        Instant receivedAt = Instant.now(clock);
        ConnectorId connectorId = authorizationPolicy.requireWriteBatch(writeMeasurements.measurements().stream()
                .map(WriteMeasurement::timeSeriesId)
                .toList());
        List<Measurement> measurements = new ArrayList<>(writeMeasurements.measurements().size());
        for (WriteMeasurement measurement : writeMeasurements.measurements()) {
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
    public MeasurementList listMeasurements(MeasurementListQuery query) {
        requireNonNull(query);
        authorizationPolicy.requireRead(query.timeSeriesId());
        List<MeasurementPageRow> rows = measurementRepository.findMeasurements(query);
        boolean truncated = rows.size() > query.limit();
        List<MeasurementPageRow> visible = truncated
                ? rows.subList(0, query.limit())
                : rows;
        MeasurementCursor nextCursor = truncated
                ? cursorOf(visible.getLast())
                : null;
        return new MeasurementList(query, truncated, nextCursor, visible);
    }

    @Override
    public MeasurementBucketList listMeasurementBuckets(MeasurementBucketQuery query) {
        requireNonNull(query);
        authorizationPolicy.requireRead(query.timeSeriesId());
        return new MeasurementBucketList(query, measurementRepository.findMeasurementBuckets(query));
    }

    @Override
    public Instant getSystemTime() {
        return measurementRepository.getSystemTime();
    }

    private static MeasurementCursor cursorOf(MeasurementPageRow measurement) {
        return new MeasurementCursor(measurement.observedAt(), measurement.submittedByConnectorId());
    }
}
