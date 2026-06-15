package at.pegelhub.connector.domain;

import at.pegelhub.contact.domain.Contact;

import java.util.Objects;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record Connector(
        ConnectorId id,
        String connectorNumber,
        Contact manufacturer,
        String typeDescription,
        String softwareVersion,
        String worksFromDataVersion,
        String dataDefinition,
        Contact softwareManufacturer,
        Contact technicallyResponsible,
        Contact operationCompany,
        String notes,
        String keycloakClientId,
        ConnectorStatus status
) {

    public Connector {
        requireNonNull(id);
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
        status = Objects.requireNonNullElse(status, ConnectorStatus.ACTIVE);
    }

    public static Connector create(
            String connectorNumber,
            Contact manufacturer,
            String typeDescription,
            String softwareVersion,
            String worksFromDataVersion,
            String dataDefinition,
            Contact softwareManufacturer,
            Contact technicallyResponsible,
            Contact operationCompany,
            String notes) {
        return new Connector(
                new ConnectorId(UUID.randomUUID()),
                connectorNumber,
                manufacturer,
                typeDescription,
                softwareVersion,
                worksFromDataVersion,
                dataDefinition,
                softwareManufacturer,
                technicallyResponsible,
                operationCompany,
                notes,
                null,
                ConnectorStatus.ACTIVE);
    }

    public Connector withId(UUID uuid) {
        return new Connector(
                new ConnectorId(uuid),
                connectorNumber, manufacturer, typeDescription, softwareVersion,
                worksFromDataVersion, dataDefinition, softwareManufacturer,
                technicallyResponsible, operationCompany, notes,
                keycloakClientId, status);
    }

    public Connector withExternalAuth(String keycloakClientId, ConnectorStatus status) {
        return new Connector(
                id, connectorNumber, manufacturer, typeDescription, softwareVersion,
                worksFromDataVersion, dataDefinition, softwareManufacturer,
                technicallyResponsible, operationCompany, notes,
                keycloakClientId, status);
    }
}
