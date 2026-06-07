package at.pegelhub.connector.application;

import at.pegelhub.connector.domain.ConnectorStatus;
import at.pegelhub.contact.api.CreateContactDto;

import static java.util.Objects.requireNonNull;

public record CreateConnectorCommand(
        String connectorNumber,
        CreateContactDto manufacturer,
        String typeDescription,
        String softwareVersion,
        String worksFromDataVersion,
        String dataDefinition,
        CreateContactDto softwareManufacturer,
        CreateContactDto technicallyResponsible,
        CreateContactDto operationCompany,
        String notes
) {

    public CreateConnectorCommand {
        requireNonNull(connectorNumber);
        requireNonNull(manufacturer);
        requireNonNull(typeDescription);
        requireNonNull(softwareVersion);
        requireNonNull(worksFromDataVersion);
        requireNonNull(dataDefinition);
        requireNonNull(softwareManufacturer);
        requireNonNull(technicallyResponsible);
        requireNonNull(operationCompany);
        requireNonNull(notes);
    }
}
