package at.pegelhub.shared.persistence;

import at.pegelhub.contact.domain.Contact;
import at.pegelhub.contact.persistence.ContactEntity;

import java.util.List;

public class EntityToDomainConverter {

    public static Contact convert(ContactEntity contact) {
        return new Contact(
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

    public static Object convert(Object object) {
        if (object instanceof ContactEntity contact) {
            return convert(contact);
        }
        return object;
    }

    @SuppressWarnings("unchecked")
    public static <S, T> List<S> convert(List<T> elements) {
        return (List<S>) elements.stream().map(EntityToDomainConverter::convert).toList();
    }
}
