package at.pegelhub.stationowner.persistence;

import at.pegelhub.stationowner.domain.StationOwner;
import at.pegelhub.stationowner.domain.StationOwnerId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Repository
class StationOwnerRepositoryAdapter implements StationOwnerRepository {

    private final SpringDataStationOwnerRepository stationOwners;

    StationOwnerRepositoryAdapter(SpringDataStationOwnerRepository stationOwners) {
        this.stationOwners = requireNonNull(stationOwners);
    }

    @Override
    public StationOwner save(StationOwner stationOwner) {
        requireNonNull(stationOwner);
        return toDomain(stationOwners.save(toEntity(stationOwner)));
    }

    @Override
    public Optional<StationOwner> findById(StationOwnerId id) {
        requireNonNull(id);
        return stationOwners.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<StationOwner> findAll() {
        return stationOwners.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    private StationOwnerEntity toEntity(StationOwner stationOwner) {
        return new StationOwnerEntity(
                stationOwner.id().value(),
                stationOwner.name(),
                stationOwner.shortName(),
                stationOwner.notes());
    }

    private StationOwner toDomain(StationOwnerEntity stationOwner) {
        return new StationOwner(
                new StationOwnerId(stationOwner.id()),
                stationOwner.name(),
                stationOwner.shortName(),
                stationOwner.notes());
    }
}
