package at.pegelhub.stationowner.application;

import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.stationowner.domain.StationOwner;
import at.pegelhub.stationowner.domain.StationOwnerId;
import at.pegelhub.stationowner.persistence.StationOwnerRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.Objects.requireNonNull;

@Service
class StationOwnerServiceImpl implements StationOwnerService {

    private final StationOwnerRepository stationOwners;

    StationOwnerServiceImpl(StationOwnerRepository stationOwners) {
        this.stationOwners = requireNonNull(stationOwners);
    }

    @Override
    public StationOwner create(CreateStationOwnerCommand command) {
        requireNonNull(command);
        return stationOwners.save(StationOwner.create(command.name(), command.shortName(), command.notes()));
    }

    @Override
    @Transactional
    public StationOwner update(StationOwnerId id, UpdateStationOwnerCommand command) {
        requireNonNull(command);
        return stationOwners.save(get(id).update(command.name(), command.shortName(), command.notes()));
    }

    @Override
    public StationOwner get(StationOwnerId id) {
        requireNonNull(id);
        return stationOwners.findById(id)
                .orElseThrow(() -> new NotFoundException("Station owner not found: " + id.value()));
    }

    @Override
    public List<StationOwner> list() {
        return stationOwners.findAll();
    }
}
