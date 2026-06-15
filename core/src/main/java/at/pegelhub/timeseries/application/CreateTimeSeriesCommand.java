package at.pegelhub.timeseries.application;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.timeseries.domain.ExternalTimeSeriesCode;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;
import at.pegelhub.timeseries.domain.UnitCode;

public record CreateTimeSeriesCommand(
        StationId stationId,
        ObservedPropertyCode observedProperty,
        UnitCode unit,
        Double referenceLevel,
        ExternalTimeSeriesCode externalCode,
        ConnectorId sourceConnectorId
) {
}
