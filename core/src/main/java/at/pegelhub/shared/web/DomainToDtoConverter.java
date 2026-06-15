package at.pegelhub.shared.web;

import at.pegelhub.contact.api.ContactDto;
import at.pegelhub.contact.domain.Contact;

import java.util.List;

/**
 * Provider, which has methods to turn domain objects to dtos.
 */
public final class DomainToDtoConverter {

    public static ContactDto convert(Contact contact) {
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
                contact.getContactNodes()
        );
    }

    public static Object convert(Object object) {
        if (object instanceof Contact contact) {
            return convert(contact);
        }
        return object;
    }

    @SuppressWarnings("unchecked")
    public static <S, T> List<S> convert(List<T> elements) {
        return (List<S>) elements.stream().map(DomainToDtoConverter::convert).toList();
    }
}
