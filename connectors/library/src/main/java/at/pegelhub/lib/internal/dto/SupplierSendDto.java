package at.pegelhub.lib.internal.dto;

/**
 * DTO to create supplier data.
 */
public record SupplierSendDto(String stationNumber, Integer stationId,
                                String stationName, String stationWater, Character stationWaterType,
                                StationManufacturerSendDto stationManufacturer, CompleteConnectorSendDto connector,
                                Integer refreshRate, Double accuracy, String mainUsage, String dataCritically,
                                //Info data
                                Double stationBaseReferenceLevel, String stationReferencePlace,
                                Double stationWaterKilometer, String stationWaterSide,
                                Double stationWaterLatitude, Double stationWaterLongitude,
                                Double stationWaterLatitudem, Double stationWaterLongitudem,
                                Double hsw100, Double hsw, Integer hswReference, Double mw,
                                Integer mwReference, Double rnw, Integer rnwReference,
                                Double hsq100, Double hsq, Double mq, Double rnq, String channelUse,
                                Boolean utcIsUsed, Boolean isSummertime) {

}
