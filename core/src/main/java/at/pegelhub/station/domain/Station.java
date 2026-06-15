package at.pegelhub.station.domain;

import at.pegelhub.stationowner.domain.StationOwnerId;

import java.util.UUID;

import static at.pegelhub.shared.validation.Validations.normalizeOptional;
import static at.pegelhub.shared.validation.Validations.normalizeRequired;
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
}
