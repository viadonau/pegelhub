package at.pegelhub.access.domain;

import at.pegelhub.connector.domain.ConnectorId;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record AccessGrant(
        AccessGrantId id,
        ConnectorId connectorId,
        AccessResourceRef resource,
        AccessPermission permission
) {

    public AccessGrant {
        requireNonNull(id);
        requireNonNull(connectorId);
        requireNonNull(resource);
        requireNonNull(permission);
    }

    public static AccessGrant create(
            ConnectorId connectorId,
            AccessResourceRef resource,
            AccessPermission permission) {
        return new AccessGrant(
                new AccessGrantId(UUID.randomUUID()),
                connectorId,
                resource,
                permission);
    }
}
