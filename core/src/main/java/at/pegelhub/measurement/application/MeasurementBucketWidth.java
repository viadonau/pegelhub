package at.pegelhub.measurement.application;

import at.pegelhub.shared.duration.PegelhubDurationLiteral;

import java.time.Duration;

import static java.util.Objects.requireNonNull;

/**
 * A fixed duration used to aggregate a Measurement time window.
 */
public record MeasurementBucketWidth(Duration duration) {

    public MeasurementBucketWidth {
        requireNonNull(duration);
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("bucket width must be positive");
        }
    }

    @Override
    public String toString() {
        return PegelhubDurationLiteral.from(duration).toString();
    }
}
