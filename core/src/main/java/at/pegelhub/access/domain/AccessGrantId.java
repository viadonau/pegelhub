package at.pegelhub.access.domain;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record AccessGrantId(UUID value) {

    public AccessGrantId {
        requireNonNull(value);
    }
}
