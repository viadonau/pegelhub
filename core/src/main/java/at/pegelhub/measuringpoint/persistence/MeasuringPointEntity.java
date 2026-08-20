package at.pegelhub.measuringpoint.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "measuring_point")
class MeasuringPointEntity {
    @Id private UUID id;
    @Column(nullable = false) private UUID stationId;
    @Column(nullable = false, length = 200) private String name;
    @Column(nullable = false, length = 8) private String status;
    @Column private BigDecimal riverKilometer;
    @Column(length = 8) private String bank;
    @Column private BigDecimal latitude;
    @Column private BigDecimal longitude;
    @Column(name = "gauge_zero_elevation_m_above_adria") private BigDecimal gaugeZeroElevationMAboveAdria;
    @Column private Integer referenceSetYear;
    @Column private BigDecimal rnwCm;
    @Column private BigDecimal mwCm;
    @Column private BigDecimal hswCm;
    @Column private BigDecimal hw100Cm;

    protected MeasuringPointEntity() { }

    MeasuringPointEntity(UUID id, UUID stationId, String name, String status,
                         BigDecimal riverKilometer, String bank, BigDecimal latitude, BigDecimal longitude,
                         BigDecimal gaugeZeroElevationMAboveAdria, Integer referenceSetYear,
                         BigDecimal rnwCm, BigDecimal mwCm, BigDecimal hswCm, BigDecimal hw100Cm) {
        this.id = id;
        this.stationId = stationId;
        this.name = name;
        this.status = status;
        this.riverKilometer = riverKilometer;
        this.bank = bank;
        this.latitude = latitude;
        this.longitude = longitude;
        this.gaugeZeroElevationMAboveAdria = gaugeZeroElevationMAboveAdria;
        this.referenceSetYear = referenceSetYear;
        this.rnwCm = rnwCm;
        this.mwCm = mwCm;
        this.hswCm = hswCm;
        this.hw100Cm = hw100Cm;
    }

    UUID id() { return id; }
    UUID stationId() { return stationId; }
    String name() { return name; }
    String status() { return status; }
    BigDecimal riverKilometer() { return riverKilometer; }
    String bank() { return bank; }
    BigDecimal latitude() { return latitude; }
    BigDecimal longitude() { return longitude; }
    BigDecimal gaugeZeroElevationMAboveAdria() { return gaugeZeroElevationMAboveAdria; }
    Integer referenceSetYear() { return referenceSetYear; }
    BigDecimal rnwCm() { return rnwCm; }
    BigDecimal mwCm() { return mwCm; }
    BigDecimal hswCm() { return hswCm; }
    BigDecimal hw100Cm() { return hw100Cm; }
}
