package com.stm.pegelhub.connector.api;

import com.stm.pegelhub.contact.api.CreateContactDto;

import java.util.UUID;

import static com.stm.pegelhub.shared.validation.Validations.requireNotEmpty;
import static com.stm.pegelhub.shared.validation.Validations.requireSEThan;

/**
 * DTO to create connector data.
 */
public record CreateConnectorDto(String connectorNumber, CreateContactDto manufacturer, String typeDescription,
                                 String softwareVersion, String worksFromDataVersion, String dataDefinition,
                                 CreateContactDto softwareManufacturer, CreateContactDto technicallyResponsible,
                                 CreateContactDto operationCompany, String notes, UUID apiToken) {
    public CreateConnectorDto {
        requireSEThan(requireNotEmpty(connectorNumber), 50);
        requireSEThan(typeDescription, 100);
        requireSEThan(softwareVersion, 20);
        requireSEThan(worksFromDataVersion, 20);
        requireSEThan(dataDefinition, 20);
        requireSEThan(notes, 255);
    }
}
