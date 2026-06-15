package at.pegelhub.stationowner.application;

import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.stationowner.domain.StationOwner;
import at.pegelhub.stationowner.domain.StationOwnerId;
import at.pegelhub.stationowner.persistence.StationOwnerRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class StationOwnerServiceImplTest {

    private static final StationOwnerId ID = new StationOwnerId(UUID.fromString("f29d9888-bb10-470e-ab22-ed98cb3ccf28"));

    private final InMemoryStationOwnerRepository repository = new InMemoryStationOwnerRepository();
    private final StationOwnerService service = new StationOwnerServiceImpl(repository);

    @Test
    void createsStationOwner() {
        var owner = service.create(new CreateStationOwnerCommand("Hydro Org", "HO", "notes"));

        assertThat(owner.id()).isNotNull();
        assertThat(owner.name()).isEqualTo("Hydro Org");
        assertThat(repository.saved).containsExactly(owner);
    }

    @Test
    void getsStationOwnerById() {
        var owner = new StationOwner(ID, "Hydro Org", null, null);
        repository.saved.add(owner);

        assertThat(service.get(ID)).isEqualTo(owner);
    }

    @Test
    void throwsNotFoundForMissingStationOwner() {
        assertThrows(NotFoundException.class, () -> service.get(ID));
    }

    @Test
    void listsStationOwners() {
        var owner = new StationOwner(ID, "Hydro Org", null, null);
        repository.saved.add(owner);

        assertThat(service.list()).containsExactly(owner);
    }

    private static final class InMemoryStationOwnerRepository implements StationOwnerRepository {

        private final List<StationOwner> saved = new ArrayList<>();

        @Override
        public StationOwner save(StationOwner stationOwner) {
            saved.add(stationOwner);
            return stationOwner;
        }

        @Override
        public Optional<StationOwner> findById(StationOwnerId id) {
            return saved.stream()
                    .filter(owner -> owner.id().equals(id))
                    .findFirst();
        }

        @Override
        public List<StationOwner> findAll() {
            return List.copyOf(saved);
        }
    }
}
