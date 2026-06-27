package at.pegelhub.measurement.application;

import java.time.Instant;

import static java.util.Objects.requireNonNull;

public record MeasurementWindow(
        Instant from,
        Instant to,
        String requested) {

    public MeasurementWindow {
        requireNonNull(from);
        requireNonNull(to);
        if (!to.isAfter(from)) {
            throw new IllegalArgumentException("to must be after from");
        }
    }
}
