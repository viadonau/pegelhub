package at.pegelhub.timeseries.domain;

import static java.util.Objects.requireNonNull;

public record UnitCode(String value) {

    public UnitCode {
        requireNonNull(value);
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("Unit code must not be blank");
        }
    }
}
