package at.pegelhub.shared.persistence;

import at.pegelhub.contact.domain.Contact;
import at.pegelhub.contact.persistence.ContactEntity;

public class DomainToEntityConverter {

    public static ContactEntity convert(Contact contact) {
        return new ContactEntity(
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
