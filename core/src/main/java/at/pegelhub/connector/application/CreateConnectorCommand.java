package at.pegelhub.connector.application;

import at.pegelhub.connector.domain.ConnectorType;

import static java.util.Objects.requireNonNull;

public record CreateConnectorCommand(String name, ConnectorType type) {
    public CreateConnectorCommand {
        requireNonNull(name);
        requireNonNull(type);
    }
}
