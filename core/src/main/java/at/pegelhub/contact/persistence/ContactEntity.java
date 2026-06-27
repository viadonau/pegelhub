package at.pegelhub.contact.persistence;

import at.pegelhub.shared.persistence.IdentifiableEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * JPA Data class for {@code Contact}s.
 */

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "Contact")
public class ContactEntity extends IdentifiableEntity {

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

    @Column
    private String contactNodes;

    public ContactEntity(UUID id, String organization, String contactPerson, String contactStreet, String contactPlz, String location, String contactCountry, String emergencyNumber, String emergencyNumberTwo, String emergencyMail, String serviceNumber, String serviceNumberTwo, String serviceMail, String administrationPhoneNumber, String administrationPhoneNumberTwo, String administrationMail, String contactNodes) {
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

    public ContactEntity() {
    }

    public String getOrganization() {
        return organization;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public String getContactStreet() {
        return contactStreet;
    }

    public String getContactPlz() {
        return contactPlz;
    }

    public String getLocation() {
        return location;
    }

    public String getContactCountry() {
        return contactCountry;
    }

    public String getEmergencyNumber() {
        return emergencyNumber;
    }

    public String getEmergencyNumberTwo() {
        return emergencyNumberTwo;
    }

    public String getEmergencyMail() {
        return emergencyMail;
    }

    public String getServiceNumber() {
        return serviceNumber;
    }

    public String getServiceNumberTwo() {
        return serviceNumberTwo;
    }

    public String getServiceMail() {
        return serviceMail;
    }

    public String getAdministrationPhoneNumber() {
        return administrationPhoneNumber;
    }

    public String getAdministrationPhoneNumberTwo() {
        return administrationPhoneNumberTwo;
    }

    public String getAdministrationMail() {
        return administrationMail;
    }

    public String getContactNodes() {
        return contactNodes;
    }
}
