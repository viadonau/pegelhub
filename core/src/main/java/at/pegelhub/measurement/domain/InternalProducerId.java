package at.pegelhub.measurement.domain;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

/** Stable identity of an in-process measurement producer, independent of its runs. */
public record InternalProducerId(UUID value) {
    public InternalProducerId {
        requireNonNull(value);
    }
}
