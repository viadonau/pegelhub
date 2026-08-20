package at.pegelhub.measuringpoint.domain;

import at.pegelhub.shared.metadata.MetadataStatus;
import at.pegelhub.station.domain.StationId;

import java.math.BigDecimal;
import java.util.UUID;

import static at.pegelhub.shared.validation.Validations.normalizeRequired;
import static java.util.Objects.requireNonNull;

public record MeasuringPoint(
        MeasuringPointId id,
        StationId stationId,
        String name,
        MetadataStatus status,
        MeasuringPointPosition position,
        BigDecimal gaugeZeroElevationMAboveAdria,
        WaterLevelReferences waterLevelReferences) {

    public MeasuringPoint {
        requireNonNull(id);
        requireNonNull(stationId);
        name = normalizeRequired(name, "Measuring point name must not be blank");
        status = status == null ? MetadataStatus.ACTIVE : status;
    }

    public static MeasuringPoint create(
            StationId stationId,
            String name,
            MetadataStatus status,
            MeasuringPointPosition position,
            BigDecimal gaugeZeroElevationMAboveAdria,
            WaterLevelReferences waterLevelReferences) {
        return new MeasuringPoint(
                new MeasuringPointId(UUID.randomUUID()),
                stationId,
                name,
                status,
                position,
                gaugeZeroElevationMAboveAdria,
                waterLevelReferences);
    }

    public MeasuringPoint update(
            String name,
            MetadataStatus status,
            MeasuringPointPosition position,
            BigDecimal gaugeZeroElevationMAboveAdria,
            WaterLevelReferences waterLevelReferences) {
        return new MeasuringPoint(
                id,
                stationId,
                name,
                status,
                position,
                gaugeZeroElevationMAboveAdria,
                waterLevelReferences);
    }
}
