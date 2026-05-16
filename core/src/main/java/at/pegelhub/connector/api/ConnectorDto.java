package at.pegelhub.connector.api;

import at.pegelhub.contact.api.ContactDto;

import java.util.UUID;

import static at.pegelhub.shared.validation.Validations.requireNotEmpty;
import static at.pegelhub.shared.validation.Validations.requireSEThan;
import static java.util.Objects.requireNonNull;

/**
 * DTO for connector data.
 */
public record ConnectorDto(UUID id, String connectorNumber, ContactDto manufacturer, String typeDescription, String softwareVersion,
                           String worksFromDataVersion, String dataDefinition,
                           ContactDto softwareManufacturer, ContactDto technicallyResponsible,
                           ContactDto operationCompany, String notes) {
    public ConnectorDto {
        requireNonNull(id);
        requireSEThan(requireNotEmpty(connectorNumber), 50);
        requireSEThan(typeDescription, 100);
        requireSEThan(softwareVersion, 20);
        requireSEThan(worksFromDataVersion, 20);
        requireSEThan(dataDefinition, 255);
        requireSEThan(notes, 255);
    }
}
