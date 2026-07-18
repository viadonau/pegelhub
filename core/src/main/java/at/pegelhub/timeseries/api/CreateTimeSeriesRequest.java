package at.pegelhub.timeseries.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Request to create observed time series metadata.")
public record CreateTimeSeriesRequest(
        @Schema(description = "Station that owns this time series.", format = "uuid", example = "2d45797c-5a99-4c52-99b5-68414f6a9b58")
        @NotNull
        UUID stationId,

        @Schema(description = "Observed property code, for example water-level or discharge.", maxLength = 120, example = "water-level")
        @NotBlank
        @Size(max = 120)
        String observedProperty,

        @Schema(description = "Unit code for measurement values.", maxLength = 40, example = "cm")
        @NotBlank
        @Size(max = 40)
        String unit,

        @Schema(description = "Optional reference level, for example meters above Adria.", example = "156.42", nullable = true)
        Double referenceLevel,

        @Schema(description = "Optional reference year for water-level metadata.", minimum = "1", maximum = "9999", example = "2020", nullable = true)
        @Min(1)
        @Max(9999)
        Integer referenceYear,

        @Schema(description = "Optional river kilometer.", example = "1933.2", nullable = true)
        Double riverKilometer,

        @Schema(description = "Optional bank description or code.", maxLength = 40, example = "left", nullable = true)
        @Size(max = 40)
        String bank,

        @Schema(description = "Optional regulatory low water value.", example = "120.0", nullable = true)
        Double rnw,

        @Schema(description = "Optional highest navigable water value.", example = "620.0", nullable = true)
        Double hsw,

        @Schema(description = "Optional mean water value.", example = "280.0", nullable = true)
        Double mw,

        @Schema(description = "Optional hundred-year flood value.", example = "760.0", nullable = true)
        Double hw100,

        @Schema(description = "Optional external connector mapping code.", maxLength = 160, example = "AT-WIEN-PEGEL-1", nullable = true)
        @Size(max = 160)
        String externalCode,

        @Schema(description = "Optional connector that is the source for this time series.", format = "uuid", example = "0d9a3c87-b41a-4663-af0a-f6ec5e6a91cf", nullable = true)
        UUID sourceConnectorId
) {
}
