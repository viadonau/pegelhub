package at.pegelhub.stationowner.domain;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record StationOwnerId(UUID value) {

    public StationOwnerId {
        requireNonNull(value);
    }
}
