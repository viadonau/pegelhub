package at.pegelhub.monitoring.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class MonitoringResponse {

    private MonitoringResponse() {
    }

    @Schema(name = "MonitoringLatestMeasurement", description = "openapi.monitoring.monitoring-latest-measurement.latest-value-in-window")
    public record LatestMeasurement(
            @Schema(description = "openapi.monitoring.monitoring-latest-measurement.observed-time") Instant observedAt,
            @Schema(description = "openapi.monitoring.monitoring-latest-measurement.observed-value") Double value) {
    }

    @Schema(name = "MonitoringMeasuringPointSummary", description = "openapi.monitoring.monitoring-measuring-point-summary.measuring-point-identity")
    public record MeasuringPointSummary(
            @Schema(description = "openapi.monitoring.monitoring-measuring-point-summary.measuring-point-identifier", format = "uuid") UUID id,
            @Schema(description = "openapi.monitoring.monitoring-measuring-point-summary.measuring-point-name") String name) {
    }

    @Schema(name = "MonitoringMeasuringPoint", description = "openapi.monitoring.monitoring-measuring-point.physical-measuring-point-metadata")
    public record MeasuringPoint(
            @Schema(description = "openapi.monitoring.monitoring-measuring-point.measuring-point-identifier", format = "uuid") UUID id,
            @Schema(description = "openapi.monitoring.monitoring-measuring-point.measuring-point-name") String name,
            @Schema(description = "openapi.monitoring.monitoring-measuring-point.reference-level", nullable = true) Double referenceLevel,
            @Schema(description = "openapi.monitoring.monitoring-measuring-point.reference-year", nullable = true) Integer referenceYear,
            @Schema(description = "openapi.monitoring.monitoring-measuring-point.river-kilometer", nullable = true) Double riverKilometer,
            @Schema(description = "openapi.monitoring.monitoring-measuring-point.bank-side", allowableValues = {"left", "right"}, nullable = true) String bank,
            @Schema(description = "openapi.monitoring.monitoring-measuring-point.regulatory-low-water", nullable = true) Double rnw,
            @Schema(description = "openapi.monitoring.monitoring-measuring-point.mean-water", nullable = true) Double mw,
            @Schema(description = "openapi.monitoring.monitoring-measuring-point.highest-navigable-water", nullable = true) Double hsw,
            @Schema(description = "openapi.monitoring.monitoring-measuring-point.hundred-year-flood", nullable = true) Double hw100) {
    }

    @Schema(name = "MonitoringStationSummary", description = "openapi.monitoring.monitoring-station-summary.station-metadata")
    public record StationSummary(
            @Schema(description = "openapi.monitoring.monitoring-station-summary.station-identifier", format = "uuid") UUID id,
            @Schema(description = "openapi.monitoring.monitoring-station-summary.station-number") String stationNumber,
            @Schema(description = "openapi.monitoring.monitoring-station-summary.station-name") String name,
            @Schema(description = "openapi.monitoring.monitoring-station-summary.water-body") String waterBody) {
    }

    @Schema(name = "MonitoringStationOwner", description = "openapi.monitoring.monitoring-station-owner.station-owner-metadata")
    public record StationOwner(
            @Schema(description = "openapi.monitoring.monitoring-station-owner.owner-identifier", format = "uuid") UUID id,
            @Schema(description = "openapi.monitoring.monitoring-station-owner.owner-name") String name,
            @Schema(description = "openapi.monitoring.monitoring-station-owner.short-name", nullable = true) String shortName) {
    }

    @Schema(name = "MonitoringTimeSeriesSummary", description = "openapi.monitoring.monitoring-time-series-summary.overview-row")
    public record TimeSeriesSummary(
            @Schema(description = "openapi.monitoring.monitoring-time-series-summary.time-series-identifier", format = "uuid") UUID id,
            @Schema(description = "openapi.monitoring.monitoring-time-series-summary.canonical-observed-property") String observedProperty,
            @Schema(description = "openapi.monitoring.monitoring-time-series-summary.unit-code") String unit,
            @Schema(description = "openapi.monitoring.monitoring-time-series-summary.measuring-point") MeasuringPointSummary measuringPoint,
            @Schema(description = "openapi.monitoring.monitoring-time-series-summary.station") StationSummary station,
            @Schema(description = "openapi.monitoring.monitoring-time-series-summary.latest-measurement", nullable = true) LatestMeasurement latestMeasurement) {
    }

    @Schema(name = "MonitoringTimeSeriesCollection", description = "openapi.monitoring.monitoring-time-series-collection.operator-monitoring-catalog")
    public record TimeSeriesCollection(
            @Schema(description = "openapi.monitoring.monitoring-time-series-collection.time-series-items") List<TimeSeriesSummary> items) {
    }

    @Schema(name = "MonitoringTimeSeriesDetail", description = "openapi.monitoring.monitoring-time-series-detail.operator-monitoring-snapshot")
    public record TimeSeriesDetail(
            @Schema(description = "openapi.monitoring.monitoring-time-series-detail.time-series-identifier", format = "uuid") UUID id,
            @Schema(description = "openapi.monitoring.monitoring-time-series-detail.canonical-observed-property") String observedProperty,
            @Schema(description = "openapi.monitoring.monitoring-time-series-detail.unit-code") String unit,
            @Schema(description = "openapi.monitoring.monitoring-time-series-detail.external-code", nullable = true) String externalCode,
            @Schema(description = "openapi.monitoring.monitoring-time-series-detail.measuring-point") MeasuringPoint measuringPoint,
            @Schema(description = "openapi.monitoring.monitoring-time-series-detail.station") StationSummary station,
            @Schema(description = "openapi.monitoring.monitoring-time-series-detail.station-owner") StationOwner stationOwner,
            @Schema(description = "openapi.monitoring.monitoring-time-series-detail.latest-measurement", nullable = true) LatestMeasurement latestMeasurement) {
    }
}
