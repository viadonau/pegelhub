package at.pegelhub.measurement.persistence;

import java.time.Instant;

record MeasurementBucketKey(Instant from, Instant to) implements Comparable<MeasurementBucketKey> {

    @Override
    public int compareTo(MeasurementBucketKey other) {
        int fromComparison = from.compareTo(other.from);
        return fromComparison != 0 ? fromComparison : to.compareTo(other.to);
    }
}
