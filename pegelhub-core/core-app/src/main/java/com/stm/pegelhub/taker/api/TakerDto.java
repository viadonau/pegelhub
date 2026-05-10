package com.stm.pegelhub.taker.api;

import com.stm.pegelhub.connector.api.ConnectorDto;

import java.util.UUID;

import static com.stm.pegelhub.shared.validation.Validations.requireNotEmpty;
import static com.stm.pegelhub.shared.validation.Validations.requireSEThan;
import static java.util.Objects.requireNonNull;

/**
 * DTO for taker data.
 */
public record TakerDto(UUID id, String stationNumber, Integer stationId,
                       TakerServiceManufacturerDto takerServiceManufacturer,
                       ConnectorDto connector, Long refreshRate) {
    public TakerDto {
        requireNonNull(id);
        requireSEThan(requireNotEmpty(stationNumber), 50);
        requireNonNull(stationId);
        requireNonNull(takerServiceManufacturer);
        requireNonNull(connector);
        requireNonNull(refreshRate);

    }
}
