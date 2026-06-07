package at.pegelhub.shared.web;

import at.pegelhub.connector.api.CreateConnectorDto;
import at.pegelhub.connector.domain.Connector;
import at.pegelhub.contact.api.CreateContactDto;
import at.pegelhub.contact.domain.Contact;

/**
 * Provider, which has methods to turn dtos to domain objects.
 */
public final class DtoToDomainConverter {

    public static Contact convert(CreateContactDto contact) {
        return new Contact(null,
                contact.organization(),
                contact.contactPerson(),
                contact.contactStreet(),
                contact.contactPlz(),
                contact.location(),
                contact.contactCountry(),
                contact.emergencyNumber(),
                contact.emergencyNumberTwo(),
                contact.emergencyMail(),
                contact.serviceNumber(),
                contact.serviceNumberTwo(),
                contact.serviceMail(),
                contact.administrationPhoneNumber(),
                contact.administrationPhoneNumberTwo(),
                contact.administrationMail(),
                contact.contactNodes()
        );
    }

    public static Connector convert(CreateConnectorDto connector) {
        return new Connector(null,
                connector.connectorNumber(),
                convert(connector.manufacturer()),
                connector.typeDescription(),
                connector.softwareVersion(),
                connector.worksFromDataVersion(),
                connector.dataDefinition(),
                convert(connector.softwareManufacturer()),
                convert(connector.technicallyResponsible()),
                convert(connector.operationCompany()),
                connector.notes());
    }

}
