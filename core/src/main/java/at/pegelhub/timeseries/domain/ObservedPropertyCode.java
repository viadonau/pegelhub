package at.pegelhub.timeseries.domain;

import static java.util.Objects.requireNonNull;

import java.util.Locale;

public record ObservedPropertyCode(String value) {

    public ObservedPropertyCode {
        requireNonNull(value, "observedProperty must not be null");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("observedProperty must not be blank");
        }
        value = canonicalize(value);
    }

    public ObservedPropertyDefinition definition() {
        return ObservedPropertyCatalog.find(value).orElseThrow();
    }

    private static String canonicalize(String value) {
        String lookup = value.toLowerCase(Locale.ROOT).replaceAll("[_\\s]+", "-");
        return switch (lookup) {
            case "w", "water-level", "waterlevel", "wasserstand", "wasser-stand" -> "water-level";
            case "wt", "water-temperature", "watertemperature", "wassertemperatur", "wasser-temperatur" -> "water-temperature";
            case "q", "discharge", "abfluss", "durchfluss", "durch-fluss" -> "discharge";
            default -> value;
        };
    }
}
