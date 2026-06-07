package at.pegelhub.shared.persistence;

import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.persistence.JpaConnector;
import at.pegelhub.contact.domain.Contact;
import at.pegelhub.contact.persistence.JpaContact;

import java.util.List;

/**
 * Maps JPA classes to their respective domain classes.
 */

public class JpaToDomainConverter {

    public static Contact convert(JpaContact jpaContact) {
        return new Contact(
                jpaContact.getId(),
                jpaContact.getOrganization(),
                jpaContact.getContactPerson(),
                jpaContact.getContactStreet(),
                jpaContact.getContactPlz(),
                jpaContact.getLocation(),
                jpaContact.getContactCountry(),
                jpaContact.getEmergencyNumber(),
                jpaContact.getEmergencyNumberTwo(),
                jpaContact.getEmergencyMail(),
                jpaContact.getServiceNumber(),
                jpaContact.getServiceNumberTwo(),
                jpaContact.getServiceMail(),
                jpaContact.getAdministrationPhoneNumber(),
                jpaContact.getAdministrationPhoneNumberTwo(),
                jpaContact.getAdministrationMail(),
                jpaContact.getContactNodes());
    }

    public static Connector convert(JpaConnector jpaConnector) {
        return new Connector(
                jpaConnector.getId(),
                jpaConnector.getConnectorNumber(),
                convert(jpaConnector.getManufacturer()),
                jpaConnector.getTypeDescription(),
                jpaConnector.getSoftwareVersion(),
                jpaConnector.getWorksFromDataVersion(),
                jpaConnector.getDataDefinition(),
                convert(jpaConnector.getSoftwareManufacturer()),
                convert(jpaConnector.getTechnicallyResponsible()),
                convert(jpaConnector.getOperatingCompany()),
                jpaConnector.getNodes(),
                jpaConnector.getKeycloakClientId(),
                jpaConnector.getStatus()
        );
    }

    public static Object convert(Object object) {
        if (object instanceof JpaContact contact) {
            return convert(contact);
        } else if (object instanceof JpaConnector connector) {
            return convert(connector);
        }
        return object;
    }

    @SuppressWarnings("unchecked")
    public static <S, T> List<S> convert(List<T> elements) {
        return (List<S>) elements.stream().map(JpaToDomainConverter::convert).toList();
    }
}
