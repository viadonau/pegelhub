package at.pegelhub.measuringpoint.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Request to create measuring point metadata.")
public record CreateMeasuringPointRequest(
        @Schema(description = "Station that contains this measuring point.", format = "uuid", example = "2d45797c-5a99-4c52-99b5-68414f6a9b58")
        @NotNull
        UUID stationId,

        @Schema(description = "Human-readable measuring point name.", maxLength = 200, example = "Main gauge")
        @NotBlank
        @Size(max = 200)
        String name,

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

        @Schema(description = "Optional mean water value.", example = "280.0", nullable = true)
        Double mw,

        @Schema(description = "Optional highest navigable water value.", example = "620.0", nullable = true)
        Double hsw,

        @Schema(description = "Optional hundred-year flood value.", example = "760.0", nullable = true)
        Double hw100
) {
}
