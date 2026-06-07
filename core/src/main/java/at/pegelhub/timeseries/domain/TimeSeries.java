package at.pegelhub.timeseries.domain;

import at.pegelhub.station.domain.StationId;

import java.time.Duration;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record TimeSeries(
        TimeSeriesId id,
        StationId stationId,
        ObservedPropertyCode observedProperty,
        UnitCode unit,
        Double referenceLevel,
        Duration expectedInterval,
        ExternalTimeSeriesCode externalCode
) {

    public TimeSeries {
        requireNonNull(id);
        requireNonNull(stationId);
        requireNonNull(observedProperty);
        requireNonNull(unit);
        if (expectedInterval != null && (expectedInterval.isZero() || expectedInterval.isNegative())) {
            throw new IllegalArgumentException("Expected interval must be positive");
        }
    }

    public static TimeSeries create(
            StationId stationId,
            ObservedPropertyCode observedProperty,
            UnitCode unit,
            Double referenceLevel,
            Duration expectedInterval,
            ExternalTimeSeriesCode externalCode) {
        return new TimeSeries(
                new TimeSeriesId(UUID.randomUUID()),
                stationId,
                observedProperty,
                unit,
                referenceLevel,
                expectedInterval,
                externalCode);
    }
}
