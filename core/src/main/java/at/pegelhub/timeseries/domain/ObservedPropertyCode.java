package at.pegelhub.timeseries.domain;

import static java.util.Objects.requireNonNull;

import java.util.Locale;

public record ObservedPropertyCode(String value) {

    public ObservedPropertyCode {
        requireNonNull(value);
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("Observed property code must not be blank");
        }
        value = canonicalize(value);
    }

    private static String canonicalize(String value) {
        String lookup = value.toLowerCase(Locale.ROOT)
                .replaceAll("[_\\s]+", "-");
        return switch (lookup) {
            case "w", "water-level", "waterlevel", "wasserstand", "wasser-stand" -> "water-level";
            case "wt", "water-temperature", "watertemperature", "wassertemperatur", "wasser-temperatur" -> "water-temperature";
            case "q", "discharge", "abfluss", "durchfluss", "durch-fluss" -> "discharge";
            default -> value;
        };
    }
}
