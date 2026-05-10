package at.pegelhub.lib.internal.dto;

/**
 * DTO to create connector data.
 */
public record CompleteConnectorSendDto(String connectorNumber, ContactSendDto manufacturer, String typeDescription, String softwareVersion,
                                String worksFromDataVersion, String dataDefinition,
                                ContactSendDto softwareManufacturer, ContactSendDto technicallyResponsible,
                                ContactSendDto operationCompany, String notes) {

}