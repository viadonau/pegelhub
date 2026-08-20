package at.pegelhub.timeseries.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

import static java.util.Objects.requireNonNull;

public enum SourceRepresentation {
    CANONICAL("canonical"),
    METRES_ABOVE_ADRIA("metres-above-adria");

    private final String value;

    SourceRepresentation(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static SourceRepresentation from(String value) {
        requireNonNull(value, "representation must not be null");
        for (SourceRepresentation representation : values()) {
            if (representation.value.equals(value.trim().toLowerCase(Locale.ROOT))) {
                return representation;
            }
        }
        throw new IllegalArgumentException("Unknown source representation: " + value);
    }
}
