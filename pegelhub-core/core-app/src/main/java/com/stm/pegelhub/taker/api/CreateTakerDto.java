package com.stm.pegelhub.taker.api;

import com.stm.pegelhub.connector.api.CreateConnectorDto;

import static com.stm.pegelhub.shared.validation.Validations.requireNotEmpty;
import static com.stm.pegelhub.shared.validation.Validations.requireSEThan;
import static java.util.Objects.requireNonNull;

/**
 * DTO to create taker data.
 */
public record CreateTakerDto(String stationNumber, Integer stationId,
                             CreateTakerServiceManufacturerDto takerServiceManufacturer,
                             CreateConnectorDto connector, Long refreshRate) {
    public CreateTakerDto {
        requireSEThan(requireNotEmpty(stationNumber), 50);
        requireNonNull(stationId);
        requireNonNull(takerServiceManufacturer);
        requireNonNull(connector);
        requireNonNull(refreshRate);

    }
}
