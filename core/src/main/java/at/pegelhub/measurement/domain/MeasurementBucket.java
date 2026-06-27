package at.pegelhub.measurement.domain;

import at.pegelhub.timeseries.domain.TimeSeriesId;

import java.time.Instant;

import static java.util.Objects.requireNonNull;

public record MeasurementBucket(
        TimeSeriesId timeSeriesId,
        Instant from,
        Instant to,
        double value,
        long sampleCount) {

    public MeasurementBucket {
        requireNonNull(timeSeriesId);
        requireNonNull(from);
        requireNonNull(to);
        if (!to.isAfter(from)) {
            throw new IllegalArgumentException("to must be after from");
        }
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("value must be finite");
        }
        if (sampleCount < 1) {
            throw new IllegalArgumentException("sampleCount must be positive");
        }
    }
}
