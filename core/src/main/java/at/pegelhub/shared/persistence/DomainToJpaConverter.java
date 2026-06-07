package at.pegelhub.shared.persistence;

import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.persistence.JpaConnector;
import at.pegelhub.contact.domain.Contact;
import at.pegelhub.contact.persistence.JpaContact;

import java.util.HashSet;
import java.util.Set;

/**
 * Maps domain classes to their respective JPA classes.
 */

public class DomainToJpaConverter {

    public static JpaContact convert(Contact contact) {
        return new JpaContact(
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

    public static JpaConnector convert(Connector connector) {
        return new JpaConnector(
                connector.getId(),
                connector.getConnectorNumber(),
                convert(connector.getManufacturer()),
                connector.getTypeDescription(),
                connector.getSoftwareVersion(),
                connector.getWorksFromDataVersion(),
                connector.getDataDefinition(),
                convert(connector.getSoftwareManufacturer()),
                convert(connector.getTechnicallyResponsible()),
                convert(connector.getOperationCompany()),
                connector.getNotes(),
                connector.getKeycloakClientId(),
                connector.getStatus()
        );
    }

    public static Set<JpaConnector> convert(Set<Connector> connector) {
        Set<JpaConnector> returnValue = new HashSet<>();
        for(Connector c: connector) {
            JpaConnector work = new JpaConnector(c.getId(),
                    c.getConnectorNumber(),
                    convert(c.getManufacturer()),
                    c.getTypeDescription(),
                    c.getSoftwareVersion(),
                    c.getWorksFromDataVersion(),
                    c.getDataDefinition(),
                    convert(c.getSoftwareManufacturer()),
                    convert(c.getTechnicallyResponsible()),
                    convert(c.getOperationCompany()),
                    c.getNotes(),
                    c.getKeycloakClientId(),
                    c.getStatus());
            returnValue.add(work);
        }
        return returnValue;
    }
}
