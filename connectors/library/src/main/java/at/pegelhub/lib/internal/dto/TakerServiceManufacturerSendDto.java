package at.pegelhub.lib.internal.dto;

/**
 * DTO to create taker service manufacturer data.
 */
public record TakerServiceManufacturerSendDto(String takerManufacturerName, String takerSystemName,
                                                String stationManufacturerFirmwareVersion, String requestRemark) {

}