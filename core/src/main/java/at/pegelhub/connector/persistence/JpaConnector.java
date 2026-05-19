package at.pegelhub.connector.persistence;

import at.pegelhub.connector.domain.ConnectorStatus;
import at.pegelhub.contact.persistence.JpaContact;
import at.pegelhub.shared.persistence.IdentifiableEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * JPA Data class for {@code Connector}s.
 */

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "Connector", uniqueConstraints = {
        @UniqueConstraint(columnNames = "keycloakClientId")
})
public class JpaConnector extends IdentifiableEntity {
    @ManyToOne
    @JoinColumn(nullable = false)
    private JpaContact manufacturer;

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
    private JpaContact softwareManufacturer;

    @ManyToOne
    @JoinColumn(nullable = false)
    private JpaContact technicallyResponsible;

    @ManyToOne
    @JoinColumn(nullable = false)
    private JpaContact operatingCompany;

    @Column()
    private String keycloakClientId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ConnectorStatus status = ConnectorStatus.ACTIVE;

    @Column
    private String nodes;

    public JpaConnector(UUID id, String connectorNumber, JpaContact manufacturer, String typeDescription, String softwareVersion, String worksFromDataVersion, String dataDefinition, JpaContact softwareManufacturer, JpaContact technicallyResponsible, JpaContact operatingCompany, String nodes) {
        this(id, connectorNumber, manufacturer, typeDescription, softwareVersion, worksFromDataVersion, dataDefinition,
                softwareManufacturer, technicallyResponsible, operatingCompany, nodes, null, ConnectorStatus.ACTIVE);
    }

    public JpaConnector(UUID id, String connectorNumber, JpaContact manufacturer, String typeDescription, String softwareVersion, String worksFromDataVersion, String dataDefinition, JpaContact softwareManufacturer, JpaContact technicallyResponsible, JpaContact operatingCompany, String nodes, String keycloakClientId, ConnectorStatus status) {
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

    public JpaConnector() {
    }
}
