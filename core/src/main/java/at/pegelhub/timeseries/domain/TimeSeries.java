package at.pegelhub.timeseries.domain;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record TimeSeries(
        TimeSeriesId id,
        MeasuringPointId measuringPointId,
        ObservedPropertyCode observedProperty,
        UnitCode unit,
        ExternalTimeSeriesCode externalCode,
        ConnectorId sourceConnectorId
) {

    public TimeSeries {
        requireNonNull(id);
        requireNonNull(measuringPointId);
        requireNonNull(observedProperty);
        requireNonNull(unit);
    }

    public static TimeSeries create(
            MeasuringPointId measuringPointId,
            ObservedPropertyCode observedProperty,
            UnitCode unit,
            ExternalTimeSeriesCode externalCode,
            ConnectorId sourceConnectorId) {
        return new TimeSeries(
                new TimeSeriesId(UUID.randomUUID()),
                measuringPointId,
                observedProperty,
                unit,
                externalCode,
                sourceConnectorId);
    }
}
