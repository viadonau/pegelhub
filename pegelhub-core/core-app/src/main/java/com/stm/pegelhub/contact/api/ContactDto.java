package com.stm.pegelhub.contact.api;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * DTO to create contact data.
 */
public record ContactDto(UUID id, String organization, String contactPerson,
                         String contactStreet, String contactPlz,
                         String location, String contactCountry,
                         String emergencyNumber, String emergencyNumberTwo,
                         String emergencyMail, String serviceNumber,
                         String serviceNumberTwo, String serviceMail,
                         String administrationPhoneNumber,
                         String administrationPhoneNumberTwo,
                         String administrationMail, String contactNodes) {
    public ContactDto {
        requireNonNull(id);
    }
}

