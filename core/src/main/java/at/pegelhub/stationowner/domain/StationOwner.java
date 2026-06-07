package at.pegelhub.stationowner.domain;

import static java.util.Objects.requireNonNull;

public record StationOwner(
        StationOwnerId id,
        String name,
        String shortName,
        String notes
) {

    public StationOwner {
        requireNonNull(id);
        name = normalizeRequired(name, "Station owner name must not be blank");
        shortName = normalizeOptional(shortName);
        notes = normalizeOptional(notes);
    }

    public static StationOwner create(String name, String shortName, String notes) {
        return new StationOwner(new StationOwnerId(java.util.UUID.randomUUID()), name, shortName, notes);
    }

    private static String normalizeRequired(String value, String message) {
        requireNonNull(value);
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        value = value.trim();
        return value.isBlank() ? null : value;
    }
}
