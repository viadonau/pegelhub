package at.pegelhub.supplier.api;

import static at.pegelhub.shared.validation.Validations.requireSEThan;

/**
 * DTO to create station manufacturer data.
 */
public record CreateStationManufacturerDto(String stationManufacturerName, String stationManufacturerType,
                                           String stationManufacturerFirmwareVersion, String stationRemark) {
    public CreateStationManufacturerDto {
        requireSEThan(stationManufacturerName, 100);
        requireSEThan(stationManufacturerType, 100);
        requireSEThan(stationManufacturerFirmwareVersion, 50);
        requireSEThan(stationRemark, 255);
    }
}
