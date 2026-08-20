package at.pegelhub.measurement.application;

import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.measurement.domain.WriteMeasurement;
import at.pegelhub.measurement.domain.WriteMeasurements;
import at.pegelhub.measurement.persistence.MeasurementRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
        MeasurementWriteAuthorization authorization = authorizationPolicy.requireWriteBatch(writeMeasurements.measurements().stream()
                .map(WriteMeasurement::timeSeriesId)
                .toList());
        List<Measurement> measurements = new ArrayList<>(writeMeasurements.measurements().size());
        for (WriteMeasurement measurement : writeMeasurements.measurements()) {
            var normalization = authorization.forTimeSeries(measurement.timeSeriesId());
            measurements.add(new Measurement(
                    measurement.timeSeriesId(),
                    measurement.observedAt(),
                    receivedAt,
                    canonicalValue(normalization, measurement.value()),
                    authorization.connectorId()));
        }
        measurementRepository.storeMeasurements(measurements);
    }

    private double canonicalValue(MeasurementWriteAuthorization.Normalization normalization, double value) {
        if (normalization.representation() != at.pegelhub.timeseries.domain.SourceRepresentation.METRES_ABOVE_ADRIA) {
            return value;
        }
        return BigDecimal.valueOf(value)
                .subtract(normalization.gaugeZeroElevationMAboveAdria())
                .movePointRight(2)
                .doubleValue();
    }

    @Override
    public MeasurementList listMeasurements(MeasurementListQuery query) {
        requireNonNull(query);
        authorizationPolicy.requireRead(query.timeSeriesId());
        return measurementRepository.listMeasurements(query);
    }

    @Override
    public MeasurementBucketList listMeasurementBuckets(MeasurementBucketQuery query) {
        requireNonNull(query);
        authorizationPolicy.requireRead(query.timeSeriesId());
        return measurementRepository.listMeasurementBuckets(query);
    }

    @Override
    public List<LatestMeasurement> listLatestMeasurements(MeasurementLatestQuery query) {
        requireNonNull(query);
        if (query.timeSeriesIds().isEmpty()) {
            return List.of();
        }
        authorizationPolicy.requireReadBatch(query.timeSeriesIds());
        return measurementRepository.listLatestMeasurements(query);
    }

    @Override
    public Instant getSystemTime() {
        return measurementRepository.getSystemTime();
    }

}
