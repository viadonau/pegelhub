package at.pegelhub.station.application;

import at.pegelhub.stationowner.domain.StationOwnerId;

public record CreateStationCommand(
        StationOwnerId ownerId,
        String stationNumber,
        String name,
        String waterBody,
        String location
) {
}
