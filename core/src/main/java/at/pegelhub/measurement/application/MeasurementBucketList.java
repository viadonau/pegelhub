package at.pegelhub.measurement.application;

import at.pegelhub.measurement.domain.MeasurementBucket;

import java.util.List;

import static java.util.Objects.requireNonNull;

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
