package at.pegelhub.monitoring.application;

import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.measurement.application.LatestMeasurement;
import at.pegelhub.station.domain.Station;
import at.pegelhub.timeseries.domain.TimeSeries;

import java.util.List;

import static java.util.Objects.requireNonNull;

public record MonitoringCollection(
        List<MonitoringTimeSeriesSummary> items) {

    public MonitoringCollection {
        requireNonNull(items);
        items = List.copyOf(items);
    }

    public record MonitoringTimeSeriesSummary(
            TimeSeries timeSeries,
            MeasuringPoint measuringPoint,
            Station station,
            LatestMeasurement latestMeasurement) {
    }
}
