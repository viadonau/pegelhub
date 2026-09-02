package at.pegelhub.measuringpoint.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "openapi.measuringpoint.water-level-references-request.water-level-reference-set")
public record WaterLevelReferencesRequest(
        @Schema(description = "openapi.measuringpoint.water-level-references-request.reference-set-year", minimum = "1", maximum = "9999", example = "2020")
        int referenceSetYear,
        @Schema(description = "openapi.measuringpoint.water-level-references-request.regulatory-low-water-centimetres", example = "120.0", nullable = true)
        BigDecimal rnwCm,
        @Schema(description = "openapi.measuringpoint.water-level-references-request.mean-water-centimetres", example = "280.0", nullable = true)
        BigDecimal mwCm,
        @Schema(description = "openapi.measuringpoint.water-level-references-request.highest-navigable-water-centimetres", example = "620.0", nullable = true)
        BigDecimal hswCm,
        @Schema(description = "openapi.measuringpoint.water-level-references-request.hundred-year-flood-centimetres", example = "760.0", nullable = true)
        BigDecimal hw100Cm) { }
