package at.pegelhub.measurement.application;

import at.pegelhub.measurement.domain.MeasurementBucket;

import java.util.List;

import static java.util.Objects.requireNonNull;

public record MeasurementBucketList(
        MeasurementBucketQuery query,
        List<MeasurementBucket> buckets) {

    public MeasurementBucketList {
        requireNonNull(query);
        buckets = List.copyOf(requireNonNull(buckets));
    }
}
