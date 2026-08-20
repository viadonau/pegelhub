package at.pegelhub.timeseries.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "time_series")
class TimeSeriesEntity {
    @Id private UUID id;
    @Column(nullable = false) private UUID measuringPointId;
    @Column(nullable = false, length = 40) private String observedProperty;
    @Column(nullable = false, length = 8) private String status;
    @Column private UUID sourceConnectorId;
    @Column(length = 32) private String sourceRepresentation;

    protected TimeSeriesEntity() { }

    TimeSeriesEntity(UUID id, UUID measuringPointId, String observedProperty, String status,
                     UUID sourceConnectorId, String sourceRepresentation) {
        this.id = id;
        this.measuringPointId = measuringPointId;
        this.observedProperty = observedProperty;
        this.status = status;
        this.sourceConnectorId = sourceConnectorId;
        this.sourceRepresentation = sourceRepresentation;
    }

    UUID id() { return id; }
    UUID measuringPointId() { return measuringPointId; }
    String observedProperty() { return observedProperty; }
    String status() { return status; }
    UUID sourceConnectorId() { return sourceConnectorId; }
    String sourceRepresentation() { return sourceRepresentation; }
}
