package at.pegelhub.timeseries.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "time_series")
class TimeSeriesEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID measuringPointId;

    @Column(nullable = false, length = 120)
    private String observedProperty;

    @Column(nullable = false, length = 40)
    private String unit;

    @Column(length = 160)
    private String externalCode;

    @Column
    private UUID sourceConnectorId;

    protected TimeSeriesEntity() {
    }

    TimeSeriesEntity(
            UUID id,
            UUID measuringPointId,
            String observedProperty,
            String unit,
            String externalCode,
            UUID sourceConnectorId) {
        this.id = id;
        this.measuringPointId = measuringPointId;
        this.observedProperty = observedProperty;
        this.unit = unit;
        this.externalCode = externalCode;
        this.sourceConnectorId = sourceConnectorId;
    }

    UUID id() {
        return id;
    }

    UUID measuringPointId() {
        return measuringPointId;
    }

    String observedProperty() {
        return observedProperty;
    }

    String unit() {
        return unit;
    }

    String externalCode() {
        return externalCode;
    }

    UUID sourceConnectorId() {
        return sourceConnectorId;
    }
}
