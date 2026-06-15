package at.pegelhub.shared.web;

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

}
