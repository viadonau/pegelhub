package at.pegelhub.connector.persistence;

import at.pegelhub.connector.domain.ConnectorStatus;
import at.pegelhub.contact.persistence.ContactEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "Connector", uniqueConstraints = {
        @UniqueConstraint(columnNames = "keycloakClientId")
})
public class ConnectorEntity {

    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private ContactEntity manufacturer;

    @Column(nullable = false, length = 50)
    private String connectorNumber;

    @Column(nullable = false, length = 100)
    private String typeDescription;

    @Column(nullable = false, length = 50)
    private String softwareVersion;

    @Column(nullable = false, length = 50)
    private String worksFromDataVersion;

    @Column(nullable = false, length = 50)
    private String dataDefinition;

    @ManyToOne
    @JoinColumn(nullable = false)
    private ContactEntity softwareManufacturer;

    @ManyToOne
    @JoinColumn(nullable = false)
    private ContactEntity technicallyResponsible;

    @ManyToOne
    @JoinColumn(nullable = false)
    private ContactEntity operatingCompany;

    @Column
    private String keycloakClientId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ConnectorStatus status = ConnectorStatus.ACTIVE;

    @Column
    private String nodes;

    public ConnectorEntity() {
    }

    public ConnectorEntity(UUID id, String connectorNumber, ContactEntity manufacturer, String typeDescription,
                 String softwareVersion, String worksFromDataVersion, String dataDefinition,
                 ContactEntity softwareManufacturer, ContactEntity technicallyResponsible,
                 ContactEntity operatingCompany, String nodes, String keycloakClientId, ConnectorStatus status) {
        this.id = id;
        this.connectorNumber = connectorNumber;
        this.manufacturer = manufacturer;
        this.typeDescription = typeDescription;
        this.softwareVersion = softwareVersion;
        this.worksFromDataVersion = worksFromDataVersion;
        this.dataDefinition = dataDefinition;
        this.softwareManufacturer = softwareManufacturer;
        this.technicallyResponsible = technicallyResponsible;
        this.operatingCompany = operatingCompany;
        this.nodes = nodes;
        this.keycloakClientId = keycloakClientId;
        this.status = status == null ? ConnectorStatus.ACTIVE : status;
    }

    UUID getId() { return id; }
    String getConnectorNumber() { return connectorNumber; }
    ContactEntity getManufacturer() { return manufacturer; }
    String getTypeDescription() { return typeDescription; }
    String getSoftwareVersion() { return softwareVersion; }
    String getWorksFromDataVersion() { return worksFromDataVersion; }
    String getDataDefinition() { return dataDefinition; }
    ContactEntity getSoftwareManufacturer() { return softwareManufacturer; }
    ContactEntity getTechnicallyResponsible() { return technicallyResponsible; }
    ContactEntity getOperatingCompany() { return operatingCompany; }
    String getKeycloakClientId() { return keycloakClientId; }
    ConnectorStatus getStatus() { return status; }
    String getNodes() { return nodes; }
    public void setKeycloakClientId(String keycloakClientId) { this.keycloakClientId = keycloakClientId; }
}
