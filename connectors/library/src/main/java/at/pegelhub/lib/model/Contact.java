package at.pegelhub.lib.model;

import java.util.UUID;

/**
 * The model class used to send and receive {@code Contact} objects.
 */
public class Contact {
    // this should be UUID but core doesn't support it yet
    private String id;
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

    public Contact() {
        this.id = "";
        this.organization = "";
        this.contactPerson = "";
        this.contactStreet = "";
        this.contactPlz = "";
        this.location = "";
        this.contactCountry = "";
        this.emergencyNumber = "";
        this.emergencyNumberTwo = "";
        this.emergencyMail = "";
        this.serviceNumber = "";
        this.serviceNumberTwo = "";
        this.serviceMail = "";
        this.administrationPhoneNumber = "";
        this.administrationPhoneNumberTwo = "";
        this.administrationMail = "";
        this.contactNodes = "";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactStreet() {
        return contactStreet;
    }

    public void setContactStreet(String contactStreet) {
        this.contactStreet = contactStreet;
    }

    public String getContactPlz() {
        return contactPlz;
    }

    public void setContactPlz(String contactPlz) {
        this.contactPlz = contactPlz;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getContactCountry() {
        return contactCountry;
    }

    public void setContactCountry(String contactCountry) {
        this.contactCountry = contactCountry;
    }

    public String getEmergencyNumber() {
        return emergencyNumber;
    }

    public void setEmergencyNumber(String emergencyNumber) {
        this.emergencyNumber = emergencyNumber;
    }

    public String getEmergencyNumberTwo() {
        return emergencyNumberTwo;
    }

    public void setEmergencyNumberTwo(String emergencyNumberTwo) {
        this.emergencyNumberTwo = emergencyNumberTwo;
    }

    public String getEmergencyMail() {
        return emergencyMail;
    }

    public void setEmergencyMail(String emergencyMail) {
        this.emergencyMail = emergencyMail;
    }

    public String getServiceNumber() {
        return serviceNumber;
    }

    public void setServiceNumber(String serviceNumber) {
        this.serviceNumber = serviceNumber;
    }

    public String getServiceNumberTwo() {
        return serviceNumberTwo;
    }

    public void setServiceNumberTwo(String serviceNumberTwo) {
        this.serviceNumberTwo = serviceNumberTwo;
    }

    public String getServiceMail() {
        return serviceMail;
    }

    public void setServiceMail(String serviceMail) {
        this.serviceMail = serviceMail;
    }

    public String getAdministrationPhoneNumber() {
        return administrationPhoneNumber;
    }

    public void setAdministrationPhoneNumber(String administrationPhoneNumber) {
        this.administrationPhoneNumber = administrationPhoneNumber;
    }

    public String getAdministrationPhoneNumberTwo() {
        return administrationPhoneNumberTwo;
    }

    public void setAdministrationPhoneNumberTwo(String administrationPhoneNumberTwo) {
        this.administrationPhoneNumberTwo = administrationPhoneNumberTwo;
    }

    public String getAdministrationMail() {
        return administrationMail;
    }

    public void setAdministrationMail(String administrationMail) {
        this.administrationMail = administrationMail;
    }

    public String getContactNodes() {
        return contactNodes;
    }

    public void setContactNodes(String contactNodes) {
        this.contactNodes = contactNodes;
    }
}
