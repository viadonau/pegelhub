package at.pegelhub.measuringpoint.api;

import at.pegelhub.shared.metadata.MetadataStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "openapi.measuringpoint.measuring-point-response.physical-measuring-point-metadata")
public record MeasuringPointResponse(
        @Schema(description = "openapi.measuringpoint.http-measuring-point-controller.measuring-point-identifier", format = "uuid", example = "58a21780-aa2f-4e1f-ae7e-5c48fd3f62dd")
        UUID id,
        @Schema(description = "openapi.measuringpoint.create-measuring-point-request.station-that-contains-this-measuring-point", format = "uuid", example = "014d58ea-fb86-4b50-bc70-ab0961736599")
        UUID stationId,
        @Schema(description = "openapi.measuringpoint.create-measuring-point-request.human-readable-measuring-point-name", example = "Main gauge")
        String name,
        @Schema(description = "openapi.shared.metadata-status")
        MetadataStatus status,
        @Schema(description = "openapi.measuringpoint.position-request.optional-physical-position", nullable = true)
        PositionResponse position,
        @Schema(description = "openapi.measuringpoint.create-measuring-point-request.optional-gauge-zero-elevation", example = "154.22", nullable = true)
        BigDecimal gaugeZeroElevationMAboveAdria,
        @Schema(description = "openapi.measuringpoint.water-level-references-request.optional-water-level-reference-set", nullable = true)
        WaterLevelReferencesResponse waterLevelReferences) {

    @Schema(description = "openapi.measuringpoint.position-request.physical-position")
    public record PositionResponse(
            @Schema(description = "openapi.measuringpoint.position-request.optional-river-kilometer", example = "1933.2", nullable = true)
            BigDecimal riverKilometer,
            @Schema(description = "openapi.measuringpoint.position-request.optional-bank-side", example = "left", nullable = true)
            String bank,
            @Schema(description = "openapi.measuringpoint.coordinates-request.optional-geographic-coordinates", nullable = true)
            CoordinatesResponse coordinates) { }

    @Schema(description = "openapi.measuringpoint.coordinates-request.geographic-coordinates")
    public record CoordinatesResponse(
            @Schema(description = "openapi.measuringpoint.coordinates-request.latitude", example = "48.25")
            BigDecimal latitude,
            @Schema(description = "openapi.measuringpoint.coordinates-request.longitude", example = "16.39")
            BigDecimal longitude) { }

    @Schema(description = "openapi.measuringpoint.water-level-references-request.water-level-reference-set")
    public record WaterLevelReferencesResponse(
            @Schema(description = "openapi.measuringpoint.water-level-references-request.reference-set-year", example = "2020")
            int referenceSetYear,
            @Schema(description = "openapi.measuringpoint.water-level-references-request.regulatory-low-water-centimetres", example = "120.0", nullable = true)
            BigDecimal rnwCm,
            @Schema(description = "openapi.measuringpoint.water-level-references-request.mean-water-centimetres", example = "280.0", nullable = true)
            BigDecimal mwCm,
            @Schema(description = "openapi.measuringpoint.water-level-references-request.highest-navigable-water-centimetres", example = "620.0", nullable = true)
            BigDecimal hswCm,
            @Schema(description = "openapi.measuringpoint.water-level-references-request.hundred-year-flood-centimetres", example = "760.0", nullable = true)
            BigDecimal hw100Cm) { }
}
