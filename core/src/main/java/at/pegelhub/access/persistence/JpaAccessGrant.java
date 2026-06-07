package at.pegelhub.access.persistence;

import at.pegelhub.access.domain.AccessPermission;
import at.pegelhub.access.domain.AccessResourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "access_grant")
class JpaAccessGrant {

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

    @Column
    private Instant validFrom;

    @Column
    private Instant validUntil;

    @Column(nullable = false)
    private boolean includeFutureTimeSeries;

    protected JpaAccessGrant() {
    }

    JpaAccessGrant(
            UUID id,
            UUID connectorId,
            AccessResourceType resourceType,
            UUID resourceId,
            AccessPermission permission,
            Instant validFrom,
            Instant validUntil,
            boolean includeFutureTimeSeries) {
        this.id = id;
        this.connectorId = connectorId;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.permission = permission;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.includeFutureTimeSeries = includeFutureTimeSeries;
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

    Instant validFrom() {
        return validFrom;
    }

    Instant validUntil() {
        return validUntil;
    }

    boolean includeFutureTimeSeries() {
        return includeFutureTimeSeries;
    }
}
