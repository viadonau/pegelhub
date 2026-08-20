package at.pegelhub.station.domain;

import at.pegelhub.shared.metadata.MetadataStatus;
import at.pegelhub.stationowner.domain.StationOwnerId;

import java.util.UUID;

import static at.pegelhub.shared.validation.Validations.normalizeRequired;
import static java.util.Objects.requireNonNull;

public record Station(
        StationId id,
        StationOwnerId ownerId,
        String name,
        String waterBody,
        MetadataStatus status) {

    public Station {
        requireNonNull(id);
        requireNonNull(ownerId);
        name = normalizeRequired(name, "Station name must not be blank");
        waterBody = normalizeRequired(waterBody, "Water body must not be blank");
        status = status == null ? MetadataStatus.ACTIVE : status;
    }

    public static Station create(StationOwnerId ownerId, String name, String waterBody) {
        return new Station(new StationId(UUID.randomUUID()), ownerId, name, waterBody, MetadataStatus.ACTIVE);
    }

    public Station update(String name, String waterBody, MetadataStatus status) {
        return new Station(id, ownerId, name, waterBody, status);
    }
}
