package at.pegelhub.measurement.application;

import static java.util.Objects.requireNonNull;

/**
 * The resolved aggregation width and, for automatic resolution, its requested point cap.
 */
public record MeasurementBucketResolution(
        MeasurementBucketWidth bucketWidth,
        Integer targetPointCount) {

    public MeasurementBucketResolution {
        requireNonNull(bucketWidth);
        if (targetPointCount != null && (targetPointCount < 1 || targetPointCount > 10_000)) {
            throw new IllegalArgumentException("targetPointCount must be between 1 and 10000");
        }
    }

    public static MeasurementBucketResolution explicit(MeasurementBucketWidth bucketWidth) {
        return new MeasurementBucketResolution(bucketWidth, null);
    }

    public static MeasurementBucketResolution automatic(MeasurementBucketWidth bucketWidth, int targetPointCount) {
        return new MeasurementBucketResolution(bucketWidth, targetPointCount);
    }
}
