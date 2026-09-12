package at.pegelhub.measurement.application;

import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.station.domain.Station;
import at.pegelhub.timeseries.domain.TimeSeries;

import static java.util.Objects.requireNonNull;

/**
 * Loaded metadata shared by permission checks and subsequent write preparation.
 */
public record MeasurementWriteTarget(
        TimeSeries timeSeries,
        MeasuringPoint measuringPoint,
        Station station) {

    public MeasurementWriteTarget {
        requireNonNull(timeSeries);
        requireNonNull(measuringPoint);
        requireNonNull(station);
        if (!timeSeries.measuringPointId().equals(measuringPoint.id())
                || !measuringPoint.stationId().equals(station.id())) {
            throw new IllegalArgumentException(
                    "Write target must contain the TimeSeries' own measuring point and station");
        }
    }
}
