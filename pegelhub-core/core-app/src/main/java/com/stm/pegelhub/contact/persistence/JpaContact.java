package com.stm.pegelhub.contact.persistence;

import com.stm.pegelhub.shared.persistence.IdentifiableEntity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

/**
 * JPA Data class for {@code Contact}s.
 */

@Entity
@Data
@Table(name = "Contact")
public class JpaContact extends IdentifiableEntity {

    @Column(length = 150)
    private String organization;

    @Column(length = 150)
    private String contactPerson;

    @Column(length = 150)
    private String contactStreet;

    @Column(length = 50)
    private String contactPlz;

    @Column(length = 50)
    private String location;

    @Column(length = 50)
    private String contactCountry;

    @Column(length = 50)
    private String emergencyNumber;

    @Column(length = 50)
    private String emergencyNumberTwo;

    @Column(length = 50)
    private String emergencyMail;

    @Column(length = 50)
    private String serviceNumber;

    @Column(length = 50)
    private String serviceNumberTwo;

    @Column(length = 50)
    private String serviceMail;

    @Column(length = 50)
    private String administrationPhoneNumber;

    @Column(length = 50)
    private String administrationPhoneNumberTwo;

    @Column(length = 50)
    private String administrationMail;

    @Column(length = 255)
    private String contactNodes;

    public JpaContact(UUID id, String organization, String contactPerson, String contactStreet, String contactPlz, String location, String contactCountry, String emergencyNumber, String emergencyNumberTwo, String emergencyMail, String serviceNumber, String serviceNumberTwo, String serviceMail, String administrationPhoneNumber, String administrationPhoneNumberTwo, String administrationMail, String contactNodes) {
        this.id = id;
        this.organization = organization;
        this.contactPerson = contactPerson;
        this.contactStreet = contactStreet;
        this.contactPlz = contactPlz;
        this.location = location;
        this.contactCountry = contactCountry;
        this.emergencyNumber = emergencyNumber;
        this.emergencyNumberTwo = emergencyNumberTwo;
        this.emergencyMail = emergencyMail;
        this.serviceNumber = serviceNumber;
        this.serviceNumberTwo = serviceNumberTwo;
        this.serviceMail = serviceMail;
        this.administrationPhoneNumber = administrationPhoneNumber;
        this.administrationPhoneNumberTwo = administrationPhoneNumberTwo;
        this.administrationMail = administrationMail;
        this.contactNodes = contactNodes;
    }

    public JpaContact() {
    }
}
