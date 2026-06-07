package at.pegelhub.timeseries.domain;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.station.domain.StationId;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record TimeSeries(
        TimeSeriesId id,
        StationId stationId,
        ObservedPropertyCode observedProperty,
        UnitCode unit,
        Double referenceLevel,
        ExternalTimeSeriesCode externalCode,
        ConnectorId sourceConnectorId
) {

    public TimeSeries {
        requireNonNull(id);
        requireNonNull(stationId);
        requireNonNull(observedProperty);
        requireNonNull(unit);
    }

    public static TimeSeries create(
            StationId stationId,
            ObservedPropertyCode observedProperty,
            UnitCode unit,
            Double referenceLevel,
            ExternalTimeSeriesCode externalCode,
            ConnectorId sourceConnectorId) {
        return new TimeSeries(
                new TimeSeriesId(UUID.randomUUID()),
                stationId,
                observedProperty,
                unit,
                referenceLevel,
                externalCode,
                sourceConnectorId);
    }
}
