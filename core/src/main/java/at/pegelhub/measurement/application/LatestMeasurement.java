package at.pegelhub.measurement.application;

import at.pegelhub.timeseries.domain.TimeSeriesId;

import java.time.Instant;

import static at.pegelhub.measurement.domain.MeasurementValues.requireFinite;
import static java.util.Objects.requireNonNull;

public record LatestMeasurement(
        TimeSeriesId timeSeriesId,
        Instant observedAt,
        double value) {

    public LatestMeasurement {
        requireNonNull(timeSeriesId);
        requireNonNull(observedAt);
        value = requireFinite(value);
    }
}
