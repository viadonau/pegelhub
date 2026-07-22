package at.pegelhub.measuringpoint.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "measuring_point")
class MeasuringPointEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID stationId;

    @Column(nullable = false, length = 200)
    private String name;

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
    private Double mw;

    @Column
    private Double hsw;

    @Column
    private Double hw100;

    protected MeasuringPointEntity() {
    }

    MeasuringPointEntity(
            UUID id,
            UUID stationId,
            String name,
            Double referenceLevel,
            Integer referenceYear,
            Double riverKilometer,
            String bank,
            Double rnw,
            Double mw,
            Double hsw,
            Double hw100) {
        this.id = id;
        this.stationId = stationId;
        this.name = name;
        this.referenceLevel = referenceLevel;
        this.referenceYear = referenceYear;
        this.riverKilometer = riverKilometer;
        this.bank = bank;
        this.rnw = rnw;
        this.mw = mw;
        this.hsw = hsw;
        this.hw100 = hw100;
    }

    UUID id() {
        return id;
    }

    UUID stationId() {
        return stationId;
    }

    String name() {
        return name;
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

    Double mw() {
        return mw;
    }

    Double hsw() {
        return hsw;
    }

    Double hw100() {
        return hw100;
    }
}
