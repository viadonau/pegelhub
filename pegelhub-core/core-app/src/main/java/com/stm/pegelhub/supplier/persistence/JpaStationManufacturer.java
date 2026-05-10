package com.stm.pegelhub.supplier.persistence;

import com.stm.pegelhub.shared.persistence.IdentifiableEntity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

/**
 * JPA Data class for {@code StationManufacturer}s.
 */
@Entity
@Data
@Table(name = "Station_Manufacturer")
public class JpaStationManufacturer extends IdentifiableEntity {

    @Column(nullable = false, length = 100)
    private String stationManufacturerName;

    @Column(nullable = false, length = 100)
    private String stationManufacturerTyp;

    @Column(nullable = false, length = 50)
    private String stationManufacturerFirmwareVersion;

    @Column(nullable = false, length = 255)
    private String stationRemark;


    public JpaStationManufacturer(UUID id, String stationManufacturerName, String stationManufacturerTyp, String stationManufacturerFirmwareVersion, String stationRemark) {
        this.id = id;
        this.stationManufacturerName = stationManufacturerName;
        this.stationManufacturerTyp = stationManufacturerTyp;
        this.stationManufacturerFirmwareVersion = stationManufacturerFirmwareVersion;
        this.stationRemark = stationRemark;
    }

    public JpaStationManufacturer() {
    }
}
