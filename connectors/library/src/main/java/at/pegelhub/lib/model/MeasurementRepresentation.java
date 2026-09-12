package at.pegelhub.lib.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Names the representations connectors can request from Core. Core does the conversions;
 * this enum only checks the representation and unit returned in the response.
 */
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

    /**
     * Checks whether Core returned the requested representation and unit.
     * Both fields are required on every response. Canonical units depend on the measured property,
     * so only their presence is checked here; other representations have a fixed unit to match.
     *
     * @throws IllegalStateException if required metadata is missing or does not match the request
     */
    public void requireResponse(String representation, String unit) {
        boolean representationMatches = value.equals(representation);
        boolean unitMatches = unit != null && !unit.isBlank()
                && (this.unit == null || this.unit.equals(unit));
        if (!representationMatches || !unitMatches) {
            throw new IllegalStateException(
                    "Core did not confirm requested measurement representation " + value);
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
