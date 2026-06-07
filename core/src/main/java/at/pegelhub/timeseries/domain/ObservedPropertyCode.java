package at.pegelhub.timeseries.domain;

import static java.util.Objects.requireNonNull;

public record ObservedPropertyCode(String value) {

    public ObservedPropertyCode {
        requireNonNull(value);
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("Observed property code must not be blank");
        }
    }
}
