package at.pegelhub.timeseries.api;

import at.pegelhub.station.domain.StationId;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.timeseries.application.CreateTimeSeriesCommand;
import at.pegelhub.timeseries.domain.ExternalTimeSeriesCode;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.UnitCode;

final class TimeSeriesMapper {

    private TimeSeriesMapper() {
    }

    static CreateTimeSeriesCommand toCommand(CreateTimeSeriesRequest request) {
        return new CreateTimeSeriesCommand(
                new StationId(request.stationId()),
                new ObservedPropertyCode(request.observedProperty()),
                new UnitCode(request.unit()),
                request.referenceLevel(),
                request.referenceYear(),
                request.riverKilometer(),
                request.bank(),
                request.rnw(),
                request.hsw(),
                request.mw(),
                request.hw100(),
                toExternalCode(request.externalCode()),
                toConnectorId(request.sourceConnectorId()));
    }

    static TimeSeriesResponse toResponse(TimeSeries timeSeries) {
        return new TimeSeriesResponse(
                timeSeries.id().value(),
                timeSeries.stationId().value(),
                timeSeries.observedProperty().value(),
                timeSeries.unit().value(),
                timeSeries.referenceLevel(),
                timeSeries.referenceYear(),
                timeSeries.riverKilometer(),
                timeSeries.bank(),
                timeSeries.rnw(),
                timeSeries.hsw(),
                timeSeries.mw(),
                timeSeries.hw100(),
                toExternalCodeValue(timeSeries.externalCode()),
                toConnectorIdValue(timeSeries.sourceConnectorId()));
    }

    private static ExternalTimeSeriesCode toExternalCode(String externalCode) {
        return externalCode == null || externalCode.isBlank() ? null : new ExternalTimeSeriesCode(externalCode);
    }

    private static String toExternalCodeValue(ExternalTimeSeriesCode externalCode) {
        return externalCode == null ? null : externalCode.value();
    }

    private static ConnectorId toConnectorId(java.util.UUID sourceConnectorId) {
        return sourceConnectorId == null ? null : new ConnectorId(sourceConnectorId);
    }

    private static java.util.UUID toConnectorIdValue(ConnectorId sourceConnectorId) {
        return sourceConnectorId == null ? null : sourceConnectorId.value();
    }
}
