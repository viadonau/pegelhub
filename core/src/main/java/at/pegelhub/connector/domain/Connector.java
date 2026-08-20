package at.pegelhub.connector.domain;

import at.pegelhub.shared.metadata.MetadataStatus;

import java.util.UUID;

import static at.pegelhub.shared.validation.Validations.normalizeOptional;
import static at.pegelhub.shared.validation.Validations.normalizeRequired;
import static java.util.Objects.requireNonNull;

public record Connector(
        ConnectorId id,
        String name,
        ConnectorType type,
        String keycloakClientId,
        MetadataStatus status) {

    public Connector {
        requireNonNull(id);
        name = normalizeRequired(name, "Connector name must not be blank");
        requireNonNull(type);
        keycloakClientId = normalizeOptional(keycloakClientId);
        status = status == null ? MetadataStatus.ACTIVE : status;
    }

    public static Connector create(String name, ConnectorType type) {
        return new Connector(new ConnectorId(UUID.randomUUID()), name, type, null, MetadataStatus.ACTIVE);
    }

    public Connector bind(String keycloakClientId, MetadataStatus status) {
        return new Connector(id, name, type, normalizeRequired(keycloakClientId, "keycloakClientId must not be blank"), status);
    }

    public Connector update(String name, ConnectorType type, MetadataStatus status) {
        return new Connector(id, name, type, keycloakClientId, status);
    }
}
