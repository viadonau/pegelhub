package at.pegelhub.measurement.persistence;

import at.pegelhub.measurement.application.MeasurementBucketQuery;
import at.pegelhub.measurement.application.MeasurementListQuery;
import at.pegelhub.measurement.application.LatestMeasurement;
import at.pegelhub.measurement.application.MeasurementLatestQuery;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.measurement.domain.MeasurementBucket;

import java.time.Instant;
import java.util.List;

/**
 * Repository for all {@code Measurement}s.
 */
public interface MeasurementRepository {

    /**
     * Saves multiple to the repository.
     *
     * @param measurements to save.
     */
    void storeMeasurements(List<Measurement> measurements);

    MeasurementPage listMeasurements(MeasurementListQuery query);

    /** Returns averages in canonical units, independent of the requested output representation. */
    List<MeasurementBucket> listMeasurementBuckets(MeasurementBucketQuery query);

    List<LatestMeasurement> listLatestMeasurements(MeasurementLatestQuery query);

    Instant getSystemTime();
}
