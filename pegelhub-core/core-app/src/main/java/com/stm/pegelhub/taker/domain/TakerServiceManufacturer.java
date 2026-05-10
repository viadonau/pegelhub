package com.stm.pegelhub.taker.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

/**
 * Data class for taker service manufacturers which represents an entry in the RDBMS
 */
@Getter
@Setter
public final class TakerServiceManufacturer {
    private final UUID id;
    private String takerManufacturerName;
    private String takerSystemName;
    private String stationManufacturerFirmwareVersion;
    private String requestRemark;

    public TakerServiceManufacturer(UUID id, String takerManufacturerName, String takerSystemName,
                                    String stationManufacturerFirmwareVersion, String requestRemark) {
        this.id = id;
        this.takerManufacturerName = takerManufacturerName;
        this.takerSystemName = takerSystemName;
        this.stationManufacturerFirmwareVersion = stationManufacturerFirmwareVersion;
        this.requestRemark = requestRemark;
    }

    public TakerServiceManufacturer() {
        this.id = null;
    }

    public TakerServiceManufacturer withId(UUID uuid) {
        return new TakerServiceManufacturer(uuid, this.takerManufacturerName, this.takerSystemName,
                this.stationManufacturerFirmwareVersion, this.requestRemark);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (TakerServiceManufacturer) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.takerManufacturerName, that.takerManufacturerName) &&
                Objects.equals(this.takerSystemName, that.takerSystemName) &&
                Objects.equals(this.stationManufacturerFirmwareVersion, that.stationManufacturerFirmwareVersion) &&
                Objects.equals(this.requestRemark, that.requestRemark);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, takerManufacturerName, takerSystemName, stationManufacturerFirmwareVersion, requestRemark);
    }

    @Override
    public String toString() {
        return "TakerServiceManufacturer[" +
                "id=" + id + ", " +
                "takerManufacturerName=" + takerManufacturerName + ", " +
                "takerSystemName=" + takerSystemName + ", " +
                "stationManufacturerFirmwareVersion=" + stationManufacturerFirmwareVersion + ", " +
                "requestRemark=" + requestRemark + ']';
    }
}