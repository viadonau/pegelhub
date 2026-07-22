package at.pegelhub.timeseries.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Request to create observed time series metadata.")
public record CreateTimeSeriesRequest(
        @Schema(description = "Measuring point that owns this time series.", format = "uuid", example = "d2de586e-0997-480b-9abe-bb2072af6689")
        @NotNull
        UUID measuringPointId,

        @Schema(description = "Observed property code, for example water-level or discharge.", maxLength = 120, example = "water-level")
        @NotBlank
        @Size(max = 120)
        String observedProperty,

        @Schema(description = "Unit code for measurement values.", maxLength = 40, example = "cm")
        @NotBlank
        @Size(max = 40)
        String unit,

        @Schema(description = "Optional external connector mapping code.", maxLength = 160, example = "AT-WIEN-PEGEL-1", nullable = true)
        @Size(max = 160)
        String externalCode,

        @Schema(description = "Optional connector that is the source for this time series.", format = "uuid", example = "0d9a3c87-b41a-4663-af0a-f6ec5e6a91cf", nullable = true)
        UUID sourceConnectorId
) {
}
