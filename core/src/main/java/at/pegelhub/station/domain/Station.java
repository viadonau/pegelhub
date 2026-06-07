package at.pegelhub.station.domain;

import at.pegelhub.stationowner.domain.StationOwnerId;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record Station(
        StationId id,
        StationOwnerId ownerId,
        String stationNumber,
        String name,
        String waterBody,
        String location
) {

    public Station {
        requireNonNull(id);
        requireNonNull(ownerId);
        stationNumber = normalizeRequired(stationNumber, "Station number must not be blank");
        name = normalizeRequired(name, "Station name must not be blank");
        waterBody = normalizeRequired(waterBody, "Water body must not be blank");
        location = normalizeOptional(location);
    }

    public static Station create(
            StationOwnerId ownerId,
            String stationNumber,
            String name,
            String waterBody,
            String location) {
        return new Station(new StationId(UUID.randomUUID()), ownerId, stationNumber, name, waterBody, location);
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
