package at.pegelhub.measurement.application;

import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.station.domain.Station;
import at.pegelhub.timeseries.domain.TimeSeries;

import static java.util.Objects.requireNonNull;

/**
 * A time series together with its measuring point and station.
 * Access checks and conversions use the same loaded objects rather than fetching them again.
 * Creating this record does not grant write access. The metadata can still change in the database
 * while these objects are being loaded.
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
