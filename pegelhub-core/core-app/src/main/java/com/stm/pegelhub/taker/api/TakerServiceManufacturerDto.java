package com.stm.pegelhub.taker.api;

import java.util.UUID;

import static com.stm.pegelhub.shared.validation.Validations.requireSEThan;
import static java.util.Objects.requireNonNull;

/**
 * DTO for taker service manufacturer data.
 */
public record TakerServiceManufacturerDto(UUID id, String takerManufacturerName, String takerSystemName,
                                          String stationManufacturerFirmwareVersion, String requestRemark
) {
    public TakerServiceManufacturerDto {
        requireNonNull(id);
        requireSEThan(takerManufacturerName, 100);
        requireSEThan(takerSystemName, 100);
        requireSEThan(stationManufacturerFirmwareVersion, 50);
        requireSEThan(requestRemark, 255);
    }
}
