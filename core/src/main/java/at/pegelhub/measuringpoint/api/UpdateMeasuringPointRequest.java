package at.pegelhub.measuringpoint.api;

import at.pegelhub.shared.metadata.MetadataStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateMeasuringPointRequest(
        @NotBlank String name,
        @NotNull MetadataStatus status,
        @Valid PositionRequest position,
        BigDecimal gaugeZeroElevationMAboveAdria,
        @Valid WaterLevelReferencesRequest waterLevelReferences) { }
