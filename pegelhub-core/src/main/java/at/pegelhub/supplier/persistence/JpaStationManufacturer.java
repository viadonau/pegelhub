package at.pegelhub.supplier.persistence;

import at.pegelhub.shared.persistence.IdentifiableEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * JPA Data class for {@code StationManufacturer}s.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "Station_Manufacturer")
public class JpaStationManufacturer extends IdentifiableEntity {

    @Column(nullable = false, length = 100)
    private String stationManufacturerName;

    @Column(nullable = false, length = 100)
    private String stationManufacturerTyp;

    @Column(nullable = false, length = 50)
    private String stationManufacturerFirmwareVersion;

    @Column(nullable = false)
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
