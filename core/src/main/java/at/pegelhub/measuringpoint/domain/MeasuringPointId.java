package at.pegelhub.measuringpoint.domain;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record MeasuringPointId(UUID value) {

    public MeasuringPointId {
        requireNonNull(value);
    }
}
