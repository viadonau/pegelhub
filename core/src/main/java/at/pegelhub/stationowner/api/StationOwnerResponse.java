package at.pegelhub.stationowner.api;

import java.util.UUID;

public record StationOwnerResponse(
        UUID id,
        String name,
        String shortName,
        String notes
) {
}
