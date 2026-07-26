package at.pegelhub.contact.persistence;

import at.pegelhub.contact.domain.Contact;

public final class ContactEntityMapper {

    private ContactEntityMapper() {
    }

    public static ContactEntity toEntity(Contact contact) {
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

    public static Contact toDomain(ContactEntity entity) {
        return new Contact(
                entity.getId(),
                entity.getOrganization(),
                entity.getContactPerson(),
                entity.getContactStreet(),
                entity.getContactPlz(),
                entity.getLocation(),
                entity.getContactCountry(),
                entity.getEmergencyNumber(),
                entity.getEmergencyNumberTwo(),
                entity.getEmergencyMail(),
                entity.getServiceNumber(),
                entity.getServiceNumberTwo(),
                entity.getServiceMail(),
                entity.getAdministrationPhoneNumber(),
                entity.getAdministrationPhoneNumberTwo(),
                entity.getAdministrationMail(),
                entity.getContactNodes());
    }
}
