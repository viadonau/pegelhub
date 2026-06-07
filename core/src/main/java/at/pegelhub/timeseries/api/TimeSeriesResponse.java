package at.pegelhub.timeseries.api;

import java.util.UUID;

public record TimeSeriesResponse(
        UUID id,
        UUID stationId,
        String observedProperty,
        String unit,
        Double referenceLevel,
        Long expectedIntervalSeconds,
        String externalCode
) {
}
