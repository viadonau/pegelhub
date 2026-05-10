package at.pegelhub.contact.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

/**
 * Data class for contacts which represents an entry in the RDBMS
 */
@Getter
@Setter
public final class Contact {

    private final UUID id;
    private String organization;
    private String contactPerson;
    private String contactStreet;
    private String contactPlz;
    private String location;
    private String contactCountry;
    private String emergencyNumber;
    private String emergencyNumberTwo;
    private String emergencyMail;
    private String serviceNumber;
    private String serviceNumberTwo;
    private String serviceMail;
    private String administrationPhoneNumber;
    private String administrationPhoneNumberTwo;
    private String administrationMail;
    private String contactNodes;

    public Contact(UUID id, String organization, String contactPerson, String contactStreet, String contactPlz, String location, String contactCountry, String emergencyNumber, String emergencyNumberTwo, String emergencyMail, String serviceNumber, String serviceNumberTwo, String serviceMail, String administrationPhoneNumber, String administrationPhoneNumberTwo, String administrationMail, String contactNodes) {
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

    public Contact() {
        this.id = null;
    }

    public Contact withId(UUID uuid) {
        return new Contact(uuid, this.organization, this.contactPerson, this.contactStreet, this.contactPlz,
                this.location, this.contactCountry, this.emergencyNumber, this.emergencyNumberTwo,
                this.emergencyMail, this.serviceNumber, this.serviceNumberTwo, this.serviceMail,
                this.administrationPhoneNumber, this.administrationPhoneNumberTwo, this.administrationMail,
                this.contactNodes);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Contact) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.organization, that.organization) &&
                Objects.equals(this.contactPerson, that.contactPerson) &&
                Objects.equals(this.contactStreet, that.contactStreet) &&
                Objects.equals(this.contactPlz, that.contactPlz) &&
                Objects.equals(this.location, that.location) &&
                Objects.equals(this.contactCountry, that.contactCountry) &&
                Objects.equals(this.emergencyNumber, that.emergencyNumber) &&
                Objects.equals(this.emergencyNumberTwo, that.emergencyNumberTwo) &&
                Objects.equals(this.emergencyMail, that.emergencyMail) &&
                Objects.equals(this.serviceNumber, that.serviceNumber) &&
                Objects.equals(this.serviceNumberTwo, that.serviceNumberTwo) &&
                Objects.equals(this.serviceMail, that.serviceMail) &&
                Objects.equals(this.administrationPhoneNumber, that.administrationPhoneNumber) &&
                Objects.equals(this.administrationPhoneNumberTwo, that.administrationPhoneNumberTwo) &&
                Objects.equals(this.administrationMail, that.administrationMail) &&
                Objects.equals(this.contactNodes, that.contactNodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, organization, contactPerson, contactStreet, contactPlz, location, contactCountry, emergencyNumber, emergencyNumberTwo, emergencyMail, serviceNumber, serviceNumberTwo, serviceMail, administrationPhoneNumber, administrationPhoneNumberTwo, administrationMail, contactNodes);
    }

    @Override
    public String toString() {
        return "Contact[" +
                "id=" + id + ", " +
                "organization=" + organization + ", " +
                "contactPerson=" + contactPerson + ", " +
                "contactStreet=" + contactStreet + ", " +
                "contactPlz=" + contactPlz + ", " +
                "location=" + location + ", " +
                "contactCountry=" + contactCountry + ", " +
                "emergencyNumber=" + emergencyNumber + ", " +
                "emergencyNumberTwo=" + emergencyNumberTwo + ", " +
                "emergencyMail=" + emergencyMail + ", " +
                "serviceNumber=" + serviceNumber + ", " +
                "serviceNumberTwo=" + serviceNumberTwo + ", " +
                "serviceMail=" + serviceMail + ", " +
                "administrationPhoneNumber=" + administrationPhoneNumber + ", " +
                "administrationPhoneNumberTwo=" + administrationPhoneNumberTwo + ", " +
                "administrationMail=" + administrationMail + ", " +
                "contactNodes=" + contactNodes + ']';
    }
}
