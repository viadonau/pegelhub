package at.pegelhub.measuringpoint.api;

import at.pegelhub.shared.metadata.MetadataStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateMeasuringPointRequest(
        @NotNull UUID stationId,
        @NotBlank String name,
        MetadataStatus status,
        @Valid PositionRequest position,
        BigDecimal gaugeZeroElevationMAboveAdria,
        @Valid WaterLevelReferencesRequest waterLevelReferences) { }
