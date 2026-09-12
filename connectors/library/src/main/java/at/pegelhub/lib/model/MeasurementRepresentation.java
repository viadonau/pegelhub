package at.pegelhub.lib.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Wire contract only. Core owns all unit and datum conversion arithmetic. */
public enum MeasurementRepresentation {
    CANONICAL("canonical", null),
    METRES_ABOVE_ADRIA("metres-above-adria", "m"),
    LITRES_PER_SECOND("litres-per-second", "l/s");

    private final String value;
    private final String unit;

    MeasurementRepresentation(String value, String unit) {
        this.value = value;
        this.unit = unit;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public void requireResponse(String representation, String unit) {
        // Canonical reads remain compatible with Core versions predating response metadata.
        if (this == CANONICAL && representation == null) {
            return;
        }
        if (!value.equals(representation) || (this.unit != null && !this.unit.equals(unit))) {
            throw new IllegalStateException("Core did not confirm requested measurement representation " + value);
        }
    }

    @JsonCreator
    public static MeasurementRepresentation from(String value) {
        for (var representation : values()) {
            if (representation.value.equals(value)) {
                return representation;
            }
        }
        throw new IllegalArgumentException("Unknown measurement representation: " + value);
    }
}
