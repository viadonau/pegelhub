package at.pegelhub.measurement.persistence;


import at.pegelhub.measurement.application.MeasurementBucketQuery;
import at.pegelhub.measurement.application.MeasurementListQuery;
import at.pegelhub.measurement.application.MeasurementPageRow;
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

    List<MeasurementPageRow> findMeasurements(MeasurementListQuery query);

    List<MeasurementBucket> findMeasurementBuckets(MeasurementBucketQuery query);

    Instant getSystemTime();
}
