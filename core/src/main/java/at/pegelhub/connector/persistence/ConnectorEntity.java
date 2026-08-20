package at.pegelhub.connector.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "connector")
class ConnectorEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(length = 255, unique = true)
    private String keycloakClientId;

    @Column(nullable = false, length = 8)
    private String status;

    protected ConnectorEntity() {
    }

    ConnectorEntity(UUID id, String name, String type, String keycloakClientId, String status) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.keycloakClientId = keycloakClientId;
        this.status = status;
    }

    UUID id() { return id; }
    String name() { return name; }
    String type() { return type; }
    String keycloakClientId() { return keycloakClientId; }
    String status() { return status; }
}
