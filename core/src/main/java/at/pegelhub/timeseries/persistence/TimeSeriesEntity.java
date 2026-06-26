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

    @Column
    private Integer referenceYear;

    @Column
    private Double riverKilometer;

    @Column(length = 40)
    private String bank;

    @Column
    private Double rnw;

    @Column
    private Double hsw;

    @Column
    private Double mw;

    @Column
    private Double hw100;

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
            Integer referenceYear,
            Double riverKilometer,
            String bank,
            Double rnw,
            Double hsw,
            Double mw,
            Double hw100,
            String externalCode,
            UUID sourceConnectorId) {
        this.id = id;
        this.stationId = stationId;
        this.observedProperty = observedProperty;
        this.unit = unit;
        this.referenceLevel = referenceLevel;
        this.referenceYear = referenceYear;
        this.riverKilometer = riverKilometer;
        this.bank = bank;
        this.rnw = rnw;
        this.hsw = hsw;
        this.mw = mw;
        this.hw100 = hw100;
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

    Integer referenceYear() {
        return referenceYear;
    }

    Double riverKilometer() {
        return riverKilometer;
    }

    String bank() {
        return bank;
    }

    Double rnw() {
        return rnw;
    }

    Double hsw() {
        return hsw;
    }

    Double mw() {
        return mw;
    }

    Double hw100() {
        return hw100;
    }

    String externalCode() {
        return externalCode;
    }

    UUID sourceConnectorId() {
        return sourceConnectorId;
    }
}
