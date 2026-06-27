package at.pegelhub.measurement.application;

import at.pegelhub.timeseries.domain.TimeSeriesId;

import static java.util.Objects.requireNonNull;

public record MeasurementListQuery(
        TimeSeriesId timeSeriesId,
        MeasurementWindow window,
        MeasurementOrder order,
        int limit,
        MeasurementCursor cursor) {

    public MeasurementListQuery {
        requireNonNull(timeSeriesId);
        requireNonNull(window);
        requireNonNull(order);
        if (limit < 1 || limit > 10_000) {
            throw new IllegalArgumentException("limit must be between 1 and 10000");
        }
    }
}
