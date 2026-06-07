package at.pegelhub.timeseries.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(
        name = "time_series",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_time_series_station_property_unit",
                columnNames = {"station_id", "observed_property", "unit"}))
class JpaTimeSeries {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID stationId;

    @Column(nullable = false, length = 120)
    private String observedProperty;

    @Column(nullable = false, length = 40)
    private String unit;

    @Column
    private Double referenceLevel;

    @Column
    private Long expectedIntervalSeconds;

    @Column(length = 160)
    private String externalCode;

    protected JpaTimeSeries() {
    }

    JpaTimeSeries(
            UUID id,
            UUID stationId,
            String observedProperty,
            String unit,
            Double referenceLevel,
            Long expectedIntervalSeconds,
            String externalCode) {
        this.id = id;
        this.stationId = stationId;
        this.observedProperty = observedProperty;
        this.unit = unit;
        this.referenceLevel = referenceLevel;
        this.expectedIntervalSeconds = expectedIntervalSeconds;
        this.externalCode = externalCode;
    }

    UUID id() {
        return id;
    }

    UUID stationId() {
        return stationId;
    }

    String observedProperty() {
        return observedProperty;
    }

    String unit() {
        return unit;
    }

    Double referenceLevel() {
        return referenceLevel;
    }

    Long expectedIntervalSeconds() {
        return expectedIntervalSeconds;
    }

    String externalCode() {
        return externalCode;
    }
}
