package at.pegelhub.lib.internal.dto;

/**
 * DTO to create station manufacturer data.
 */
public record StationManufacturerSendDto(String stationManufacturerName, String stationManufacturerType,
                                           String stationManufacturerFirmwareVersion, String stationRemark) {
}

