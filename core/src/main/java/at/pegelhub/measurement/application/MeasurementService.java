package at.pegelhub.measurement.application;

import at.pegelhub.measurement.domain.WriteMeasurements;

import java.time.Instant;
import java.util.List;

/**
 * Reads and writes measurements, checks access, and converts values to and from storage units.
 */
public interface MeasurementService {

    /**
     * Uses the input representation configured on each time series. Individual writes do not specify a unit.
     * Every access check and conversion must succeed before the batch is passed to the repository.
     * If the database write then fails, this service does not roll back any values already stored.
     */
    void writeMeasurements(WriteMeasurements measurements);

    /**
     * Returns values in the requested representation, regardless of the source's input representation.
     * Unsupported conversions or missing required metadata are errors even when no measurements match.
     */
    MeasurementList listMeasurements(MeasurementListQuery query);

    /**
     * Returns averages in the requested representation. Conversion leaves each bucket's time range
     * and sample count unchanged. The conversion must be valid even if there are no buckets to return.
     */
    MeasurementBucketList listMeasurementBuckets(MeasurementBucketQuery query);

    /** Returns the latest values in storage units for monitoring. This method does not convert output values. */
    List<LatestMeasurement> listLatestMeasurements(MeasurementLatestQuery query);

    Instant getSystemTime();
}
