package at.pegelhub.monitoring.api;

import at.pegelhub.shared.metadata.MetadataStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class MonitoringResponse {
    private MonitoringResponse() { }

    public record LatestMeasurement(Instant observedAt, Double value) { }
    public record MeasuringPointSummary(UUID id, String name) { }
    public record MeasuringPoint(
            UUID id,
            String name,
            MetadataStatus status,
            Position position,
            BigDecimal gaugeZeroElevationMAboveAdria,
            WaterLevelReferences waterLevelReferences) { }
    public record Position(BigDecimal riverKilometer, String bank) { }
    public record WaterLevelReferences(
            int referenceSetYear, BigDecimal rnwCm, BigDecimal mwCm, BigDecimal hswCm, BigDecimal hw100Cm) { }
    public record StationSummary(UUID id, String name, String waterBody) { }
    public record StationOwner(UUID id, String name, String shortName) { }
    public record TimeSeriesSummary(
            UUID id, String observedProperty, String unit,
            MeasuringPointSummary measuringPoint, StationSummary station, LatestMeasurement latestMeasurement) { }
    public record TimeSeriesCollection(List<TimeSeriesSummary> items) { }
    public record TimeSeriesDetail(
            UUID id,
            String observedProperty,
            String unit,
            MetadataStatus status,
            MeasuringPoint measuringPoint,
            StationSummary station,
            StationOwner stationOwner,
            LatestMeasurement latestMeasurement) { }
}
