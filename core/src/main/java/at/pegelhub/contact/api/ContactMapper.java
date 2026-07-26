package at.pegelhub.contact.api;

import at.pegelhub.contact.domain.Contact;

import java.util.List;

public final class ContactMapper {

    private ContactMapper() {
    }

    public static Contact toDomain(CreateContactDto contact) {
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
                contact.contactNodes());
    }

    public static ContactDto toResponse(Contact contact) {
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

    public static List<ContactDto> toResponses(List<Contact> contacts) {
        return contacts.stream().map(ContactMapper::toResponse).toList();
    }
}
