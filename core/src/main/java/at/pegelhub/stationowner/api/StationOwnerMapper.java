package at.pegelhub.stationowner.api;

import at.pegelhub.stationowner.application.CreateStationOwnerCommand;
import at.pegelhub.stationowner.domain.StationOwner;

final class StationOwnerMapper {

    private StationOwnerMapper() {
    }

    static CreateStationOwnerCommand toCommand(CreateStationOwnerRequest request) {
        return new CreateStationOwnerCommand(request.name(), request.shortName(), request.notes());
    }

    static StationOwnerResponse toResponse(StationOwner owner) {
        return new StationOwnerResponse(owner.id().value(), owner.name(), owner.shortName(), owner.notes());
    }
}
