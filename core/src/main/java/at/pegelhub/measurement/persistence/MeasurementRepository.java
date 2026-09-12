package at.pegelhub.measurement.persistence;

import at.pegelhub.measurement.application.MeasurementBucketQuery;
import at.pegelhub.measurement.application.MeasurementListQuery;
import at.pegelhub.measurement.application.LatestMeasurement;
import at.pegelhub.measurement.application.MeasurementLatestQuery;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.measurement.domain.MeasurementBucket;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Reads and writes values in canonical storage units. The application service handles conversion.
 * A representation in a read query does not change the units returned here.
 */
public interface MeasurementRepository {

    /**
     * The caller must check permissions and convert values to storage units before calling this method.
     */
    void storeMeasurements(List<Measurement> measurements);

    /** Limits this Influx call without changing ingestion's shared client; timeout does not prove rejection. */
    void storeMeasurements(List<Measurement> measurements, Duration timeout);

    MeasurementPage listMeasurements(MeasurementListQuery query);

    /** Limits this call, not an entire multi-query window. Callers must still inspect the result's truncation flag. */
    MeasurementPage listMeasurements(MeasurementListQuery query, Duration timeout);

    /** Returns averages in storage units, even if the query asks for a different representation. */
    List<MeasurementBucket> listMeasurementBuckets(MeasurementBucketQuery query);

    List<LatestMeasurement> listLatestMeasurements(MeasurementLatestQuery query);

    Instant getSystemTime();
}
