package at.pegelhub.measuringpoint.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "openapi.measuringpoint.create-measuring-point-request.request-to-create-measuring-point-metadata")
public record CreateMeasuringPointRequest(
        @Schema(description = "openapi.measuringpoint.create-measuring-point-request.station-that-contains-this-measuring-point", format = "uuid", example = "2d45797c-5a99-4c52-99b5-68414f6a9b58")
        @NotNull
        UUID stationId,

        @Schema(description = "openapi.measuringpoint.create-measuring-point-request.human-readable-measuring-point-name", maxLength = 200, example = "Main gauge")
        @NotBlank
        @Size(max = 200)
        String name,

        @Schema(description = "openapi.measuringpoint.create-measuring-point-request.optional-reference-level-for-example-meters-above", example = "156.42", nullable = true)
        Double referenceLevel,

        @Schema(description = "openapi.measuringpoint.create-measuring-point-request.optional-reference-year-for-water-level-metadata", minimum = "1", maximum = "9999", example = "2020", nullable = true)
        @Min(1)
        @Max(9999)
        Integer referenceYear,

        @Schema(description = "openapi.measuringpoint.create-measuring-point-request.optional-river-kilometer", example = "1933.2", nullable = true)
        Double riverKilometer,

        @Schema(description = "openapi.measuringpoint.create-measuring-point-request.optional-bank-description-or-code", maxLength = 40, example = "left", nullable = true)
        @Size(max = 40)
        String bank,

        @Schema(description = "openapi.measuringpoint.create-measuring-point-request.optional-regulatory-low-water-value", example = "120.0", nullable = true)
        Double rnw,

        @Schema(description = "openapi.measuringpoint.create-measuring-point-request.optional-mean-water-value", example = "280.0", nullable = true)
        Double mw,

        @Schema(description = "openapi.measuringpoint.create-measuring-point-request.optional-highest-navigable-water-value", example = "620.0", nullable = true)
        Double hsw,

        @Schema(description = "openapi.measuringpoint.create-measuring-point-request.optional-hundred-year-flood-value", example = "760.0", nullable = true)
        Double hw100
) {
}
