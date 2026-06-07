package at.pegelhub.timeseries.application;

import at.pegelhub.station.domain.StationId;
import at.pegelhub.timeseries.domain.ExternalTimeSeriesCode;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;
import at.pegelhub.timeseries.domain.UnitCode;

import java.time.Duration;

public record CreateTimeSeriesCommand(
        StationId stationId,
        ObservedPropertyCode observedProperty,
        UnitCode unit,
        Double referenceLevel,
        Duration expectedInterval,
        ExternalTimeSeriesCode externalCode
) {
}
