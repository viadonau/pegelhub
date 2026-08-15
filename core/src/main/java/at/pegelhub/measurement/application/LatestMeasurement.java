package at.pegelhub.measurement.application;

import at.pegelhub.timeseries.domain.TimeSeriesId;

import java.time.Instant;

import static java.util.Objects.requireNonNull;

public record LatestMeasurement(
        TimeSeriesId timeSeriesId,
        Instant observedAt,
        double value) {

    public LatestMeasurement {
        requireNonNull(timeSeriesId);
        requireNonNull(observedAt);
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("value must be finite");
        }
    }
}
