package at.pegelhub.stationowner.application;

import at.pegelhub.stationowner.domain.StationOwner;
import at.pegelhub.stationowner.domain.StationOwnerId;

import java.util.List;

public interface StationOwnerService {

    StationOwner create(CreateStationOwnerCommand command);

    StationOwner get(StationOwnerId id);

    List<StationOwner> list();
}
