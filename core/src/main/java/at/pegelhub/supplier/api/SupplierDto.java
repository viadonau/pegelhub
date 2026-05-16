package at.pegelhub.supplier.api;

import at.pegelhub.connector.api.ConnectorDto;


import java.util.UUID;

import static at.pegelhub.shared.validation.Validations.*;
import static java.util.Objects.requireNonNull;


/**
 * DTO for supplier data.
 */
public record SupplierDto(UUID id, String stationNumber, Integer stationId,
                          String stationName, String stationWater, Character stationWaterType,
                          StationManufacturerDto stationManufacturer, ConnectorDto connector,
                          Long refreshRate, Double accuracy, String mainUsage, String dataCritically,
                          //Info data
                          Double stationBaseReferenceLevel, String stationReferencePlace,
                          Double stationWaterKilometer, String stationWaterSide,
                          Double stationWaterLatitude, Double stationWaterLongitude,
                          Double stationWaterLatitudem, Double stationWaterLongitudem,
                          Double hsw100, Double hsw, Integer hswReference, Double mw,
                          Integer mwReference, Double rnw, Integer rnwReference,
                          Double hsq100, Double hsq, Double mq, Double rnq, String channelUse) {
    public SupplierDto {
        requireNonNull(id);
        requireSEThan(requireNotEmpty(stationNumber), 50);
        requireNonNull(stationId);
        requireSEThan(requireNotEmpty(stationName), 255);
        requireSEThan(requireNotEmpty(stationWater), 255);
        requireNonNull(stationWaterType);
        requireNonNull(stationManufacturer);
        requireNonNull(connector);
        requireNonNull(refreshRate);
        requireSEThan(requirePositive(accuracy), 100);
        requireSEThan(requireNotEmpty(mainUsage), 255);
        requireSEThan(requireNotEmpty(dataCritically), 20);
    }
}
