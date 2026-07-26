package at.pegelhub.access.persistence;

import at.pegelhub.access.domain.AccessPermission;
import at.pegelhub.access.domain.AccessResourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(
        name = "access_grant",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_access_grant_assignment",
                columnNames = {"connector_id", "resource_type", "resource_id", "permission"}),
        indexes = {
                @Index(name = "ix_access_grant_connector", columnList = "connector_id"),
                @Index(name = "ix_access_grant_resource", columnList = "resource_type, resource_id")
        })
class AccessGrantEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "connector_id", nullable = false)
    private UUID connectorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 40)
    private AccessResourceType resourceType;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false, length = 40)
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
