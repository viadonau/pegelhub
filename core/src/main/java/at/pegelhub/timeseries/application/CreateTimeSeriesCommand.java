package at.pegelhub.timeseries.application;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.timeseries.domain.ExternalTimeSeriesCode;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;
import at.pegelhub.timeseries.domain.UnitCode;

public record CreateTimeSeriesCommand(
        MeasuringPointId measuringPointId,
        ObservedPropertyCode observedProperty,
        UnitCode unit,
        ExternalTimeSeriesCode externalCode,
        ConnectorId sourceConnectorId
) {
}
