package at.pegelhub.station.application;

import at.pegelhub.stationowner.domain.StationOwnerId;

public record CreateStationCommand(StationOwnerId ownerId, String name, String waterBody) { }
