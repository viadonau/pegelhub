package at.pegelhub.access.persistence;

import at.pegelhub.access.domain.AccessPermission;
import at.pegelhub.access.domain.AccessResourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "access_grant")
class AccessGrantEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID connectorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AccessResourceType resourceType;

    @Column(nullable = false)
    private UUID resourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AccessPermission permission;

    protected AccessGrantEntity() {
    }

    AccessGrantEntity(
            UUID id,
            UUID connectorId,
            AccessResourceType resourceType,
            UUID resourceId,
            AccessPermission permission) {
        this.id = id;
        this.connectorId = connectorId;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.permission = permission;
    }

    UUID id() {
        return id;
    }

    UUID connectorId() {
        return connectorId;
    }

    AccessResourceType resourceType() {
        return resourceType;
    }

    UUID resourceId() {
        return resourceId;
    }

    AccessPermission permission() {
        return permission;
    }
}
