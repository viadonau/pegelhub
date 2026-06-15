package at.pegelhub.stationowner.domain;

import static at.pegelhub.shared.validation.Validations.normalizeOptional;
import static at.pegelhub.shared.validation.Validations.normalizeRequired;
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
}
