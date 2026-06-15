package at.pegelhub.connector.api;

import at.pegelhub.connector.application.CreateConnectorCommand;
import at.pegelhub.connector.domain.Connector;
import at.pegelhub.contact.api.ContactDto;
import at.pegelhub.contact.domain.Contact;

final class ConnectorMapper {

    private ConnectorMapper() {
    }

    static ConnectorDto toResponse(Connector connector) {
        return new ConnectorDto(
                connector.id().value(),
                connector.connectorNumber(),
                toContactDto(connector.manufacturer()),
                connector.typeDescription(),
                connector.softwareVersion(),
                connector.worksFromDataVersion(),
                connector.dataDefinition(),
                toContactDto(connector.softwareManufacturer()),
                toContactDto(connector.technicallyResponsible()),
                toContactDto(connector.operationCompany()),
                connector.notes(),
                connector.keycloakClientId(),
                connector.status());
    }

    static CreateConnectorCommand toCommand(CreateConnectorDto dto) {
        return new CreateConnectorCommand(
                dto.connectorNumber(),
                dto.manufacturer(),
                dto.typeDescription(),
                dto.softwareVersion(),
                dto.worksFromDataVersion(),
                dto.dataDefinition(),
                dto.softwareManufacturer(),
                dto.technicallyResponsible(),
                dto.operationCompany(),
                dto.notes());
    }

    private static ContactDto toContactDto(Contact contact) {
        return new ContactDto(
                contact.getId(),
                contact.getOrganization(),
                contact.getContactPerson(),
                contact.getContactStreet(),
                contact.getContactPlz(),
                contact.getLocation(),
                contact.getContactCountry(),
                contact.getEmergencyNumber(),
                contact.getEmergencyNumberTwo(),
                contact.getEmergencyMail(),
                contact.getServiceNumber(),
                contact.getServiceNumberTwo(),
                contact.getServiceMail(),
                contact.getAdministrationPhoneNumber(),
                contact.getAdministrationPhoneNumberTwo(),
                contact.getAdministrationMail(),
                contact.getContactNodes());
    }
}
