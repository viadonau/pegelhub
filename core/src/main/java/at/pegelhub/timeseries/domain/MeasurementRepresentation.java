package at.pegelhub.timeseries.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

import static java.util.Objects.requireNonNull;

/**
 * Describes a value's unit and reference level. For example, metres above Adria specifies
 * both a unit and the level from which the height is measured.
 * The source assignment defines the input representation. A read query can request a different
 * output representation. Neither changes the time series' storage unit.
 *
 * @see ObservedPropertyCatalog
 */
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
