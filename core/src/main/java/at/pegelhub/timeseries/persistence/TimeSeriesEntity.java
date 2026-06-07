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
class TimeSeriesEntity {

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

    @Column(length = 160)
    private String externalCode;

    @Column
    private UUID sourceConnectorId;

    protected TimeSeriesEntity() {
    }

    TimeSeriesEntity(
            UUID id,
            UUID stationId,
            String observedProperty,
            String unit,
            Double referenceLevel,
            String externalCode,
            UUID sourceConnectorId) {
        this.id = id;
        this.stationId = stationId;
        this.observedProperty = observedProperty;
        this.unit = unit;
        this.referenceLevel = referenceLevel;
        this.externalCode = externalCode;
        this.sourceConnectorId = sourceConnectorId;
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

    String externalCode() {
        return externalCode;
    }

    UUID sourceConnectorId() {
        return sourceConnectorId;
    }
}
