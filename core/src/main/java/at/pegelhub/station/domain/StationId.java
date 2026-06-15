package at.pegelhub.station.domain;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record StationId(UUID value) {

    public StationId {
        requireNonNull(value);
    }
}
