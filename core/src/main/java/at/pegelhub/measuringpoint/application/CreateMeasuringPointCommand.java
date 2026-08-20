package at.pegelhub.measuringpoint.application;

import at.pegelhub.measuringpoint.domain.MeasuringPointPosition;
import at.pegelhub.measuringpoint.domain.WaterLevelReferences;
import at.pegelhub.shared.metadata.MetadataStatus;
import at.pegelhub.station.domain.StationId;

import java.math.BigDecimal;

public record CreateMeasuringPointCommand(
        StationId stationId,
        String name,
        MetadataStatus status,
        MeasuringPointPosition position,
        BigDecimal gaugeZeroElevationMAboveAdria,
        WaterLevelReferences waterLevelReferences) { }
