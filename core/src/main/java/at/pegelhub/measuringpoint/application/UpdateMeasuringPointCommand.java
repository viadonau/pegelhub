package at.pegelhub.measuringpoint.application;

import at.pegelhub.measuringpoint.domain.MeasuringPointPosition;
import at.pegelhub.measuringpoint.domain.WaterLevelReferences;
import at.pegelhub.shared.metadata.MetadataStatus;

import java.math.BigDecimal;

public record UpdateMeasuringPointCommand(
        String name,
        MetadataStatus status,
        MeasuringPointPosition position,
        BigDecimal gaugeZeroElevationMAboveAdria,
        WaterLevelReferences waterLevelReferences) { }
