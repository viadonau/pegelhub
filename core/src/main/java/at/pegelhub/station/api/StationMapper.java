package at.pegelhub.station.api;

import at.pegelhub.station.application.CreateStationCommand;
import at.pegelhub.station.application.UpdateStationCommand;
import at.pegelhub.station.domain.Station;
import at.pegelhub.stationowner.domain.StationOwnerId;

final class StationMapper {
    private StationMapper() { }
    static CreateStationCommand toCommand(CreateStationRequest request) {
        return new CreateStationCommand(new StationOwnerId(request.ownerId()), request.name(), request.waterBody());
    }
    static UpdateStationCommand toCommand(UpdateStationRequest request) {
        return new UpdateStationCommand(request.name(), request.waterBody(), request.status());
    }
    static StationResponse toResponse(Station station) {
        return new StationResponse(station.id().value(), station.ownerId().value(), station.name(), station.waterBody(), station.status());
    }
}
