package at.pegelhub.connector.application;

import at.pegelhub.connector.domain.ConnectorType;
import at.pegelhub.shared.metadata.MetadataStatus;

import static java.util.Objects.requireNonNull;

public record UpdateConnectorCommand(String name, ConnectorType type, MetadataStatus status) {
    public UpdateConnectorCommand {
        requireNonNull(name);
        requireNonNull(type);
        requireNonNull(status);
    }
}
