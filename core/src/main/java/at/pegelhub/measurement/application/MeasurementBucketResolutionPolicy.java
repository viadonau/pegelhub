package at.pegelhub.measurement.application;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Chooses a fixed aggregation width that keeps chart responses within the requested point cap.
 */
@Component
public class MeasurementBucketResolutionPolicy {

    private static final List<MeasurementBucketWidth> CANDIDATES = List.of(
            width(Duration.ofSeconds(1)),
            width(Duration.ofSeconds(5)),
            width(Duration.ofSeconds(10)),
            width(Duration.ofSeconds(30)),
            width(Duration.ofMinutes(1)),
            width(Duration.ofMinutes(5)),
            width(Duration.ofMinutes(15)),
            width(Duration.ofMinutes(30)),
            width(Duration.ofHours(1)),
            width(Duration.ofHours(6)),
            width(Duration.ofHours(12)),
            width(Duration.ofDays(1)),
            width(Duration.ofDays(7)));

    public MeasurementBucketResolution automatic(MeasurementWindow window, int targetPointCount) {
        requireNonNull(window);
        if (targetPointCount < 1 || targetPointCount > 10_000) {
            throw new IllegalArgumentException("maxPoints must be between 1 and 10000");
        }

        Duration windowDuration = Duration.between(window.from(), window.to());
        MeasurementBucketWidth width = CANDIDATES.stream()
                .filter(candidate -> pointCount(windowDuration, candidate.duration()) <= targetPointCount)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Requested time window cannot be resolved within maxPoints"));
        return MeasurementBucketResolution.automatic(width, targetPointCount);
    }

    private static long pointCount(Duration window, Duration bucket) {
        return Math.ceilDiv(window.toMillis(), Math.max(1, bucket.toMillis()));
    }

    private static MeasurementBucketWidth width(Duration duration) {
        return new MeasurementBucketWidth(duration);
    }
}
