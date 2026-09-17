package at.pegelhub.connector.tstp;

import at.pegelhub.lib.model.MeasurementRepresentation;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;

/** TSTP parameter names and the wire units supported by Core. No conversion happens here. */
public enum TstpParameter {
    WATER_LEVEL("Wasserstand", "cm"),
    WATER_TEMPERATURE("WTemperatur", "\u00b0C"),
    DISCHARGE("Abfluss", "m3/s", "m^3/s", "m\u00b3/s", "l/s");

    private final String value;
    private final List<String> units;

    TstpParameter(String value, String... units) {
        this.value = value;
        this.units = List.of(units);
    }

    @JsonValue
    public String value() {
        return value;
    }

    public String defaultUnit() {
        return units.getFirst();
    }

    public MeasurementRepresentation representation(String unit) {
        if (!units.contains(unit)) {
            throw new IllegalArgumentException("Unsupported TSTP unit " + unit + " for parameter " + value);
        }
        return "l/s".equals(unit)
                ? MeasurementRepresentation.LITRES_PER_SECOND
                : MeasurementRepresentation.CANONICAL;
    }

    @JsonCreator
    public static TstpParameter from(String value) {
        for (TstpParameter parameter : values()) {
            if (parameter.value.equals(value)) {
                return parameter;
            }
        }
        throw new IllegalArgumentException("Unknown TSTP parameter: " + value);
    }
}
