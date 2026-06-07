package at.pegelhub.stationowner.application;

import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.stationowner.domain.StationOwner;
import at.pegelhub.stationowner.domain.StationOwnerId;
import at.pegelhub.stationowner.persistence.StationOwnerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.Objects.requireNonNull;

@Service
final class StationOwnerServiceImpl implements StationOwnerService {

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
