package at.pegelhub.lib.internal.dto;

/**
 * DTO to create taker data.
 */
public record TakerSendDto(String stationNumber, Integer stationId,
                             TakerServiceManufacturerSendDto takerServiceManufacturer,
                             CompleteConnectorSendDto connector, Integer refreshRate) {

}
