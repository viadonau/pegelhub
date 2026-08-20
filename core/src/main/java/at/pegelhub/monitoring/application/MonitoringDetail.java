package at.pegelhub.monitoring.application;

import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.measurement.application.LatestMeasurement;
import at.pegelhub.shared.metadata.MetadataStatus;
import at.pegelhub.station.domain.Station;
import at.pegelhub.stationowner.domain.StationOwner;
import at.pegelhub.timeseries.domain.TimeSeries;

import static java.util.Objects.requireNonNull;

public record MonitoringDetail(
        TimeSeries timeSeries,
        MeasuringPoint measuringPoint,
        Station station,
        StationOwner stationOwner,
        MetadataStatus status,
        LatestMeasurement latestMeasurement) {

    public MonitoringDetail {
        requireNonNull(timeSeries);
        requireNonNull(measuringPoint);
        requireNonNull(station);
        requireNonNull(stationOwner);
        requireNonNull(status);
    }
}
