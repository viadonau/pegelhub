package at.pegelhub.connector.domain;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record ConnectorId(UUID value) {

    public ConnectorId {
        requireNonNull(value);
    }
}
