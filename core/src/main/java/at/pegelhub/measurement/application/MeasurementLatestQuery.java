package at.pegelhub.measurement.application;

import at.pegelhub.timeseries.domain.TimeSeriesId;

import java.util.LinkedHashSet;
import java.util.List;

import static java.util.Objects.requireNonNull;

/** A bounded batch read for the newest value of several time series. */
public record MeasurementLatestQuery(
        List<TimeSeriesId> timeSeriesIds,
        MeasurementWindow window) {

    public MeasurementLatestQuery {
        requireNonNull(timeSeriesIds);
        requireNonNull(window);
        timeSeriesIds = List.copyOf(new LinkedHashSet<>(timeSeriesIds.stream()
                .map(id -> requireNonNull(id))
                .toList()));
    }
}
