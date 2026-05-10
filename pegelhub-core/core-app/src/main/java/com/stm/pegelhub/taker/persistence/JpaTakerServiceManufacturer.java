package com.stm.pegelhub.taker.persistence;

import com.stm.pegelhub.shared.persistence.IdentifiableEntity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

/**
 * JPA Data class for {@code TakerServiceManufacturer}s.
 */

@Entity
@Data
@Table(name = "Taker_Service_Manufacturer")
public class JpaTakerServiceManufacturer extends IdentifiableEntity {

    @Column(nullable = false, length = 100)
    private String takerManufacturerName;

    @Column(nullable = false, length = 100)
    private String takerSystemName;

    @Column(nullable = false, length = 50)
    private String stationManufacturerFirmwareVersion;

    @Column(nullable = false, length = 255)
    private String requestRemark;

    public JpaTakerServiceManufacturer(UUID id, String takerManufacturerName, String takerSystemName, String stationManufacturerFirmwareVersion, String requestRemark) {
        this.id = id;
        this.takerManufacturerName = takerManufacturerName;
        this.takerSystemName = takerSystemName;
        this.stationManufacturerFirmwareVersion = stationManufacturerFirmwareVersion;
        this.requestRemark = requestRemark;
    }

    public JpaTakerServiceManufacturer(String uuid) {
        setId(UUID.fromString(uuid));
    }

    public JpaTakerServiceManufacturer() {
    }
}
