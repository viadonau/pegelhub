package at.pegelhub.connector.domain;

import at.pegelhub.contact.domain.Contact;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;


/**
 * Data class for connectors which represents an entry in the RDBMS.
 */
@Getter
@Setter
public final class Connector {
    private final UUID id;
    private String connectorNumber;
    private Contact manufacturer;
    private String typeDescription;
    private String softwareVersion;
    private String worksFromDataVersion;
    private String dataDefinition;
    private Contact softwareManufacturer;
    private Contact technicallyResponsible;
    private Contact operationCompany;
    private String notes;
    private UUID apiToken;

    public Connector(UUID id, String connectorNumber, Contact manufacturer, String typeDescription,
                     String softwareVersion, String worksFromDataVersion, String dataDefinition,
                     Contact softwareManufacturer, Contact technicallyResponsible, Contact operationCompany,
                     String notes, UUID apiToken) {
        this.id = id;
        this.connectorNumber = connectorNumber;
        this.manufacturer = manufacturer;
        this.typeDescription = typeDescription;
        this.softwareVersion = softwareVersion;
        this.worksFromDataVersion = worksFromDataVersion;
        this.dataDefinition = dataDefinition;
        this.softwareManufacturer = softwareManufacturer;
        this.technicallyResponsible = technicallyResponsible;
        this.operationCompany = operationCompany;
        this.notes = notes;
        this.apiToken =  apiToken;
    }

    public Connector() {
        this.id = null;
    }

    public Connector withId(UUID uuid) {
        return new Connector(uuid, this.connectorNumber, this.manufacturer, this.typeDescription, this.softwareVersion,
                this.worksFromDataVersion, this.dataDefinition, this.softwareManufacturer, this.technicallyResponsible,
                this.operationCompany, this.notes, this.apiToken);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Connector) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.connectorNumber, that.connectorNumber) &&
                Objects.equals(this.manufacturer, that.manufacturer) &&
                Objects.equals(this.typeDescription, that.typeDescription) &&
                Objects.equals(this.softwareVersion, that.softwareVersion) &&
                Objects.equals(this.worksFromDataVersion, that.worksFromDataVersion) &&
                Objects.equals(this.dataDefinition, that.dataDefinition) &&
                Objects.equals(this.softwareManufacturer, that.softwareManufacturer) &&
                Objects.equals(this.technicallyResponsible, that.technicallyResponsible) &&
                Objects.equals(this.operationCompany, that.operationCompany) &&
                Objects.equals(this.notes, that.notes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, connectorNumber, manufacturer, typeDescription, softwareVersion, worksFromDataVersion,
                dataDefinition, softwareManufacturer, technicallyResponsible, operationCompany, notes);
    }

    @Override
    public String toString() {
        return "Connector[" +
                "id=" + id + ", " +
                "manufacturer=" + manufacturer + ", " +
                "connectorNUmber=" + connectorNumber + ", " +
                "typeDescription=" + typeDescription + ", " +
                "softwareVersion=" + softwareVersion + ", " +
                "worksFromDataVersion=" + worksFromDataVersion + ", " +
                "dataDefinition=" + dataDefinition + ", " +
                "softwareManufacturer=" + softwareManufacturer + ", " +
                "technicallyResponsible=" + technicallyResponsible + ", " +
                "operationCompany=" + operationCompany + ", " +
                "notes=" + notes + ']';
    }
}
