package at.pegelhub.measurement.application;

import at.pegelhub.measurement.domain.MeasurementBucket;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * These averages have already been converted to the query's representation.
 * Conversion does not change the time ranges or sample counts.
 */
public record MeasurementBucketList(
        MeasurementBucketQuery query,
        List<MeasurementBucket> buckets,
        String unit) {

    public MeasurementBucketList {
        requireNonNull(query);
        requireNonNull(unit);
        buckets = List.copyOf(requireNonNull(buckets));
    }
}
