package at.pegelhub.measurement.application;

import java.time.Instant;

public record MeasurementInterval(
        Instant from,
        Instant to,
        Double mean,
        long observationCount,
        long supportedNanos,
        Instant lastContributingObservedAt,
        String windowStatus,
        String supportStatus) {
}
