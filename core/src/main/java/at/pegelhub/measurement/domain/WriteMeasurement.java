package at.pegelhub.measurement.domain;

import at.pegelhub.timeseries.domain.TimeSeriesId;

import java.time.Instant;

import static java.util.Objects.requireNonNull;

/**
 * A scalar observation submitted for one TimeSeries.
 */
public record WriteMeasurement(TimeSeriesId timeSeriesId, Instant observedAt, double value) {

    public WriteMeasurement {
        requireNonNull(timeSeriesId);
        requireNonNull(observedAt);
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("value must be finite");
        }
    }
}
