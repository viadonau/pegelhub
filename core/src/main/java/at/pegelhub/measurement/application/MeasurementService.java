package at.pegelhub.measurement.application;

import at.pegelhub.measurement.domain.WriteMeasurements;

import java.time.Instant;

/**
 * Service for all {@code Measurement}s.
 */
public interface MeasurementService {

    void writeMeasurements(WriteMeasurements measurements);

    MeasurementList listMeasurements(MeasurementListQuery query);

    MeasurementBucketList listMeasurementBuckets(MeasurementBucketQuery query);

    Instant getSystemTime();
}
