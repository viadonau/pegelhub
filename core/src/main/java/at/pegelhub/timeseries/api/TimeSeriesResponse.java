package at.pegelhub.timeseries.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Observed time series metadata.")
public record TimeSeriesResponse(
        @Schema(description = "Time series identifier.", format = "uuid", example = "8ce8c5b6-f093-4d46-b770-7239cdfa3d76")
        UUID id,
        @Schema(description = "Station that owns this time series.", format = "uuid", example = "2d45797c-5a99-4c52-99b5-68414f6a9b58")
        UUID stationId,
        @Schema(description = "Observed property code.", example = "water-level")
        String observedProperty,
        @Schema(description = "Unit code for measurement values.", example = "cm")
        String unit,
        @Schema(description = "Optional reference level, for example meters above Adria.", example = "156.42", nullable = true)
        Double referenceLevel,
        @Schema(description = "Optional reference year for water-level metadata.", example = "2020", nullable = true)
        Integer referenceYear,
        @Schema(description = "Optional river kilometer.", example = "1933.2", nullable = true)
        Double riverKilometer,
        @Schema(description = "Optional bank description or code.", example = "left", nullable = true)
        String bank,
        @Schema(description = "Optional regulatory low water value.", example = "120.0", nullable = true)
        Double rnw,
        @Schema(description = "Optional highest navigable water value.", example = "620.0", nullable = true)
        Double hsw,
        @Schema(description = "Optional mean water value.", example = "280.0", nullable = true)
        Double mw,
        @Schema(description = "Optional hundred-year flood value.", example = "760.0", nullable = true)
        Double hw100,
        @Schema(description = "Optional external connector mapping code.", example = "AT-WIEN-PEGEL-1", nullable = true)
        String externalCode,
        @Schema(description = "Optional connector that is the source for this time series.", format = "uuid", example = "0d9a3c87-b41a-4663-af0a-f6ec5e6a91cf", nullable = true)
        UUID sourceConnectorId
) {
}
