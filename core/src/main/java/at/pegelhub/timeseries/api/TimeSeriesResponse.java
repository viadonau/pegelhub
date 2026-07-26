package at.pegelhub.timeseries.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "openapi.timeseries.time-series-response.observed-time-series-metadata")
public record TimeSeriesResponse(
        @Schema(description = "openapi.measurement.measurement-api.time-series-identifier", format = "uuid", example = "8ce8c5b6-f093-4d46-b770-7239cdfa3d76")
        UUID id,
        @Schema(description = "openapi.timeseries.create-time-series-request.measuring-point-that-owns-this-time-series", format = "uuid", example = "d2de586e-0997-480b-9abe-bb2072af6689")
        UUID measuringPointId,
        @Schema(description = "openapi.timeseries.time-series-response.observed-property-code", example = "water-level")
        String observedProperty,
        @Schema(description = "openapi.timeseries.create-time-series-request.unit-code-for-measurement-values", example = "cm")
        String unit,
        @Schema(description = "openapi.timeseries.create-time-series-request.optional-external-connector-mapping-code", example = "AT-WIEN-PEGEL-1", nullable = true)
        String externalCode,
        @Schema(description = "openapi.timeseries.create-time-series-request.optional-connector-that-is-the-source-for", format = "uuid", example = "0d9a3c87-b41a-4663-af0a-f6ec5e6a91cf", nullable = true)
        UUID sourceConnectorId
) {
}
