package at.pegelhub.timeseries.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

import static java.util.Objects.requireNonNull;

public enum MeasurementRepresentation {
    CANONICAL("canonical"),
    METRES_ABOVE_ADRIA("metres-above-adria"),
    LITRES_PER_SECOND("litres-per-second");

    private final String value;

    MeasurementRepresentation(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static MeasurementRepresentation from(String value) {
        requireNonNull(value, "representation must not be null");
        for (MeasurementRepresentation representation : values()) {
            if (representation.value.equals(value.trim().toLowerCase(Locale.ROOT))) {
                return representation;
            }
        }
        throw new IllegalArgumentException("Unknown measurement representation: " + value);
    }
}
