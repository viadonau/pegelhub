package at.pegelhub.measurement.domain;

import at.pegelhub.timeseries.domain.TimeSeriesId;

import java.time.Instant;

import static java.util.Objects.requireNonNull;

public record MeasurementAverage(
        TimeSeriesId timeSeriesId,
        Instant rangeStart,
        Instant rangeEnd,
        double value,
        long sampleCount) {

    public MeasurementAverage {
        requireNonNull(timeSeriesId);
        requireNonNull(rangeStart);
        requireNonNull(rangeEnd);
        if (!rangeEnd.isAfter(rangeStart)) {
            throw new IllegalArgumentException("rangeEnd must be after rangeStart");
        }
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("value must be finite");
        }
        if (sampleCount < 1) {
            throw new IllegalArgumentException("sampleCount must be positive");
        }
    }
}
