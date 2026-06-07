package at.pegelhub.station.api;

import java.util.UUID;

public record StationResponse(
        UUID id,
        UUID ownerId,
        String stationNumber,
        String name,
        String waterBody,
        String location
) {
}
