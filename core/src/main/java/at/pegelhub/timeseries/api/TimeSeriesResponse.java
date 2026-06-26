package at.pegelhub.timeseries.api;

import java.util.UUID;

public record TimeSeriesResponse(
        UUID id,
        UUID stationId,
        String observedProperty,
        String unit,
        Double referenceLevel,
        Integer referenceYear,
        Double riverKilometer,
        String bank,
        Double rnw,
        Double hsw,
        Double mw,
        Double hw100,
        String externalCode,
        UUID sourceConnectorId
) {
}
