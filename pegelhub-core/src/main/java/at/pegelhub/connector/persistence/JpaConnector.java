package at.pegelhub.connector.persistence;

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
@Table(name = "Connector", uniqueConstraints = @UniqueConstraint(columnNames = "apiToken"))
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

    @Column(nullable = false)
    private UUID apiToken;

    @Column
    private String nodes;

    public JpaConnector(UUID id, String connectorNumber, JpaContact manufacturer, String typeDescription, String softwareVersion, String worksFromDataVersion, String dataDefinition, JpaContact softwareManufacturer, JpaContact technicallyResponsible, JpaContact operatingCompany, String nodes, UUID apiToken) {
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
        this.apiToken = apiToken;
    }

    public JpaConnector() {
    }
}
