package at.pegelhub.measurement.application;

import at.pegelhub.timeseries.domain.MeasurementRepresentation;
import at.pegelhub.timeseries.domain.TimeSeriesId;

import static java.util.Objects.requireNonNull;

public record MeasurementBucketQuery(
        TimeSeriesId timeSeriesId,
        MeasurementWindow window,
        MeasurementBucketResolution resolution,
        MeasurementRepresentation representation) {

    public MeasurementBucketQuery(
            TimeSeriesId timeSeriesId,
            MeasurementWindow window,
            MeasurementBucketResolution resolution) {
        this(timeSeriesId, window, resolution, MeasurementRepresentation.CANONICAL);
    }

    public MeasurementBucketQuery {
        requireNonNull(timeSeriesId);
        requireNonNull(window);
        requireNonNull(resolution);
        requireNonNull(representation);
    }
}
