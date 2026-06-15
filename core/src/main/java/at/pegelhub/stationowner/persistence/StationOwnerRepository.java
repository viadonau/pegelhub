package at.pegelhub.stationowner.persistence;

import at.pegelhub.stationowner.domain.StationOwner;
import at.pegelhub.stationowner.domain.StationOwnerId;

import java.util.List;
import java.util.Optional;

public interface StationOwnerRepository {

    StationOwner save(StationOwner stationOwner);

    Optional<StationOwner> findById(StationOwnerId id);

    List<StationOwner> findAll();
}
