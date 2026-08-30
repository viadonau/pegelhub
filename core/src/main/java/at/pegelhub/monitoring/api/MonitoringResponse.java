package at.pegelhub.monitoring.api;

import at.pegelhub.shared.metadata.MetadataStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class MonitoringResponse {
    private MonitoringResponse() { }

    @Schema(description = "openapi.monitoring.monitoring-latest-measurement.latest-value-in-window")
    public record LatestMeasurement(
            @Schema(description = "openapi.monitoring.monitoring-latest-measurement.observed-time", example = "2026-06-17T12:00:00Z")
            Instant observedAt,
            @Schema(description = "openapi.monitoring.monitoring-latest-measurement.observed-value", example = "273.0")
            Double value) { }

    @Schema(description = "openapi.monitoring.monitoring-measuring-point-summary.measuring-point-identity")
    public record MeasuringPointSummary(
            @Schema(description = "openapi.monitoring.monitoring-measuring-point-summary.measuring-point-identifier", format = "uuid", example = "58a21780-aa2f-4e1f-ae7e-5c48fd3f62dd")
            UUID id,
            @Schema(description = "openapi.monitoring.monitoring-measuring-point-summary.measuring-point-name", example = "Main gauge")
            String name) { }

    @Schema(description = "openapi.monitoring.monitoring-measuring-point.physical-measuring-point-metadata")
    public record MeasuringPoint(
            @Schema(description = "openapi.monitoring.monitoring-measuring-point.measuring-point-identifier", format = "uuid", example = "58a21780-aa2f-4e1f-ae7e-5c48fd3f62dd")
            UUID id,
            @Schema(description = "openapi.monitoring.monitoring-measuring-point.measuring-point-name", example = "Main gauge")
            String name,
            @Schema(description = "openapi.monitoring.monitoring-measuring-point.lifecycle-status")
            MetadataStatus status,
            @Schema(description = "openapi.monitoring.monitoring-measuring-point.position", nullable = true)
            Position position,
            @Schema(description = "openapi.monitoring.monitoring-measuring-point.gauge-zero-elevation", example = "154.22", nullable = true)
            BigDecimal gaugeZeroElevationMAboveAdria,
            @Schema(description = "openapi.monitoring.monitoring-measuring-point.water-level-references", nullable = true)
            WaterLevelReferences waterLevelReferences) { }

    @Schema(description = "openapi.monitoring.monitoring-position.physical-position")
    public record Position(
            @Schema(description = "openapi.monitoring.monitoring-position.river-kilometer", example = "1933.2", nullable = true)
            BigDecimal riverKilometer,
            @Schema(description = "openapi.monitoring.monitoring-position.bank-side", example = "left", nullable = true)
            String bank) { }

    @Schema(description = "openapi.monitoring.monitoring-water-level-references.reference-set")
    public record WaterLevelReferences(
            @Schema(description = "openapi.monitoring.monitoring-water-level-references.reference-set-year", example = "2020")
            int referenceSetYear,
            @Schema(description = "openapi.monitoring.monitoring-water-level-references.regulatory-low-water-centimetres", example = "120.0", nullable = true)
            BigDecimal rnwCm,
            @Schema(description = "openapi.monitoring.monitoring-water-level-references.mean-water-centimetres", example = "280.0", nullable = true)
            BigDecimal mwCm,
            @Schema(description = "openapi.monitoring.monitoring-water-level-references.highest-navigable-water-centimetres", example = "620.0", nullable = true)
            BigDecimal hswCm,
            @Schema(description = "openapi.monitoring.monitoring-water-level-references.hundred-year-flood-centimetres", example = "760.0", nullable = true)
            BigDecimal hw100Cm) { }

    @Schema(description = "openapi.monitoring.monitoring-station-summary.station-metadata")
    public record StationSummary(
            @Schema(description = "openapi.monitoring.monitoring-station-summary.station-identifier", format = "uuid", example = "014d58ea-fb86-4b50-bc70-ab0961736599")
            UUID id,
            @Schema(description = "openapi.monitoring.monitoring-station-summary.station-name", example = "Korneuburg")
            String name,
            @Schema(description = "openapi.monitoring.monitoring-station-summary.water-body", example = "Danube")
            String waterBody) { }

    @Schema(description = "openapi.monitoring.monitoring-station-owner.station-owner-metadata")
    public record StationOwner(
            @Schema(description = "openapi.monitoring.monitoring-station-owner.owner-identifier", format = "uuid", example = "c45a9e77-33da-460e-ae82-dc5fc78923c5")
            UUID id,
            @Schema(description = "openapi.monitoring.monitoring-station-owner.owner-name", example = "Hydrographic Service Vienna")
            String name,
            @Schema(description = "openapi.monitoring.monitoring-station-owner.short-name", example = "HS Vienna", nullable = true)
            String shortName) { }

    @Schema(description = "openapi.monitoring.monitoring-time-series-summary.overview-row")
    public record TimeSeriesSummary(
            @Schema(description = "openapi.monitoring.monitoring-time-series-summary.time-series-identifier", format = "uuid", example = "8ce8c5b6-f093-4d46-b770-7239cdfa3d76")
            UUID id,
            @Schema(description = "openapi.monitoring.monitoring-time-series-summary.canonical-observed-property", example = "water-level")
            String observedProperty,
            @Schema(description = "openapi.monitoring.monitoring-time-series-summary.unit-code", example = "cm")
            String unit,
            @Schema(description = "openapi.monitoring.monitoring-time-series-summary.measuring-point")
            MeasuringPointSummary measuringPoint,
            @Schema(description = "openapi.monitoring.monitoring-time-series-summary.station")
            StationSummary station,
            @Schema(description = "openapi.monitoring.monitoring-time-series-summary.latest-measurement", nullable = true)
            LatestMeasurement latestMeasurement) { }

    @Schema(description = "openapi.monitoring.monitoring-time-series-collection.operator-monitoring-catalog")
    public record TimeSeriesCollection(
            @Schema(description = "openapi.monitoring.monitoring-time-series-collection.time-series-items")
            List<TimeSeriesSummary> items) { }

    @Schema(description = "openapi.monitoring.monitoring-time-series-detail.operator-monitoring-snapshot")
    public record TimeSeriesDetail(
            @Schema(description = "openapi.monitoring.monitoring-time-series-detail.time-series-identifier", format = "uuid", example = "8ce8c5b6-f093-4d46-b770-7239cdfa3d76")
            UUID id,
            @Schema(description = "openapi.monitoring.monitoring-time-series-detail.canonical-observed-property", example = "water-level")
            String observedProperty,
            @Schema(description = "openapi.monitoring.monitoring-time-series-detail.unit-code", example = "cm")
            String unit,
            @Schema(description = "openapi.monitoring.monitoring-time-series-detail.effective-status")
            MetadataStatus status,
            @Schema(description = "openapi.monitoring.monitoring-time-series-detail.measuring-point")
            MeasuringPoint measuringPoint,
            @Schema(description = "openapi.monitoring.monitoring-time-series-detail.station")
            StationSummary station,
            @Schema(description = "openapi.monitoring.monitoring-time-series-detail.station-owner")
            StationOwner stationOwner,
            @Schema(description = "openapi.monitoring.monitoring-time-series-detail.latest-measurement", nullable = true)
            LatestMeasurement latestMeasurement) { }
}
