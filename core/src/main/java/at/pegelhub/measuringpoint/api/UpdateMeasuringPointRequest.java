package at.pegelhub.measuringpoint.api;

import at.pegelhub.shared.metadata.MetadataStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "openapi.measuringpoint.update-measuring-point-request.request-to-replace-measuring-point-metadata")
public record UpdateMeasuringPointRequest(
        @Schema(description = "openapi.measuringpoint.create-measuring-point-request.human-readable-measuring-point-name", example = "Main gauge")
        @NotBlank String name,
        @Schema(description = "openapi.shared.metadata-status")
        @NotNull MetadataStatus status,
        @Schema(description = "openapi.measuringpoint.position-request.optional-physical-position", nullable = true)
        @Valid PositionRequest position,
        @Schema(description = "openapi.measuringpoint.create-measuring-point-request.optional-gauge-zero-elevation", example = "154.22", nullable = true)
        BigDecimal gaugeZeroElevationMAboveAdria,
        @Schema(description = "openapi.measuringpoint.water-level-references-request.optional-water-level-reference-set", nullable = true)
        @Valid WaterLevelReferencesRequest waterLevelReferences) { }
