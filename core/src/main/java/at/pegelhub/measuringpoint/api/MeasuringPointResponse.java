package at.pegelhub.measuringpoint.api;

import at.pegelhub.shared.metadata.MetadataStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record MeasuringPointResponse(
        UUID id,
        UUID stationId,
        String name,
        MetadataStatus status,
        PositionResponse position,
        BigDecimal gaugeZeroElevationMAboveAdria,
        WaterLevelReferencesResponse waterLevelReferences) {

    public record PositionResponse(BigDecimal riverKilometer, String bank, CoordinatesResponse coordinates) { }
    public record CoordinatesResponse(BigDecimal latitude, BigDecimal longitude) { }
    public record WaterLevelReferencesResponse(
            int referenceSetYear, BigDecimal rnwCm, BigDecimal mwCm, BigDecimal hswCm, BigDecimal hw100Cm) { }
}
