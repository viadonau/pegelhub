package at.pegelhub.supplier.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

/**
 * Data class for station manufacturers which represents an entry in the RDBMS
 */
@Getter
@Setter
public final class StationManufacturer {
    private final UUID id;
    private String stationManufacturerName;
    private String stationManufacturerType;
    private String stationManufacturerFirmwareVersion;
    private String stationRemark;

    public StationManufacturer(UUID id, String stationManufacturerName, String stationManufacturerType,
                               String stationManufacturerFirmwareVersion, String stationRemark) {
        this.id = id;
        this.stationManufacturerName = stationManufacturerName;
        this.stationManufacturerType = stationManufacturerType;
        this.stationManufacturerFirmwareVersion = stationManufacturerFirmwareVersion;
        this.stationRemark = stationRemark;
    }

    public StationManufacturer() {
        this.id = null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (StationManufacturer) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.stationManufacturerName, that.stationManufacturerName) &&
                Objects.equals(this.stationManufacturerType, that.stationManufacturerType) &&
                Objects.equals(this.stationManufacturerFirmwareVersion, that.stationManufacturerFirmwareVersion) &&
                Objects.equals(this.stationRemark, that.stationRemark);
    }

    public StationManufacturer withId(UUID uuid) {
        return new StationManufacturer(uuid, this.stationManufacturerName, this.stationManufacturerType,
                this.stationManufacturerFirmwareVersion, this.stationRemark);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, stationManufacturerName, stationManufacturerType, stationManufacturerFirmwareVersion, stationRemark);
    }

    @Override
    public String toString() {
        return "StationManufacturer[" +
                "id=" + id + ", " +
                "stationManufacturerName=" + stationManufacturerName + ", " +
                "stationManufacturerType=" + stationManufacturerType + ", " +
                "stationManufacturerFirmwareVersion=" + stationManufacturerFirmwareVersion + ", " +
                "stationRemark=" + stationRemark + ']';
    }
}