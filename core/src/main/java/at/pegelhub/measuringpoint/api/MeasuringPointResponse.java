package at.pegelhub.measuringpoint.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Physical measuring point metadata.")
public record MeasuringPointResponse(
        @Schema(description = "Measuring point identifier.", format = "uuid", example = "d2de586e-0997-480b-9abe-bb2072af6689")
        UUID id,
        @Schema(description = "Station that contains this measuring point.", format = "uuid", example = "2d45797c-5a99-4c52-99b5-68414f6a9b58")
        UUID stationId,
        @Schema(description = "Human-readable measuring point name.", example = "Main gauge")
        String name,
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
        @Schema(description = "Optional mean water value.", example = "280.0", nullable = true)
        Double mw,
        @Schema(description = "Optional highest navigable water value.", example = "620.0", nullable = true)
        Double hsw,
        @Schema(description = "Optional hundred-year flood value.", example = "760.0", nullable = true)
        Double hw100
) {
}
