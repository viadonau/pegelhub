package at.pegelhub.measurement.application;

import at.pegelhub.timeseries.domain.TimeSeriesId;

import static java.util.Objects.requireNonNull;

public record MeasurementBucketQuery(
        TimeSeriesId timeSeriesId,
        MeasurementWindow window,
        MeasurementBucketResolution resolution) {

    public MeasurementBucketQuery {
        requireNonNull(timeSeriesId);
        requireNonNull(window);
        requireNonNull(resolution);
    }
}
