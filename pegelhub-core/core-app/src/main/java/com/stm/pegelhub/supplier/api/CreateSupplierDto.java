package com.stm.pegelhub.supplier.api;

import com.stm.pegelhub.connector.api.CreateConnectorDto;


import static com.stm.pegelhub.shared.validation.Validations.*;
import static java.util.Objects.requireNonNull;


/**
 * DTO to create supplier data.
 */
public record CreateSupplierDto(String stationNumber, Integer stationId,
                                String stationName, String stationWater, Character stationWaterType,
                                CreateStationManufacturerDto stationManufacturer, CreateConnectorDto connector,
                                Long refreshRate, Double accuracy, String mainUsage, String dataCritically,
                                //Info data
                                Double stationBaseReferenceLevel, String stationReferencePlace,
                                Double stationWaterKilometer, String stationWaterSide,
                                Double stationWaterLatitude, Double stationWaterLongitude,
                                Double stationWaterLatitudem, Double stationWaterLongitudem,
                                Double hsw100, Double hsw, Integer hswReference, Double mw,
                                Integer mwReference, Double rnw, Integer rnwReference,
                                Double hsq100, Double hsq, Double mq, Double rnq, String channelUse,
                                Boolean utcIsUsed, Boolean isSummertime) {
    public CreateSupplierDto {
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
