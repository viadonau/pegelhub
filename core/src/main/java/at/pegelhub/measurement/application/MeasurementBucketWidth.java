package at.pegelhub.measurement.application;

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
        if (duration.toNanos() % Duration.ofDays(1).toNanos() == 0) {
            return duration.toDays() + "d";
        }
        if (duration.toNanos() % Duration.ofHours(1).toNanos() == 0) {
            return duration.toHours() + "h";
        }
        if (duration.toNanos() % Duration.ofMinutes(1).toNanos() == 0) {
            return duration.toMinutes() + "m";
        }
        if (duration.toNanos() % Duration.ofSeconds(1).toNanos() == 0) {
            return duration.toSeconds() + "s";
        }
        if (duration.toNanos() % Duration.ofMillis(1).toNanos() == 0) {
            return duration.toMillis() + "ms";
        }
        return duration.toNanos() + "ns";
    }
}
