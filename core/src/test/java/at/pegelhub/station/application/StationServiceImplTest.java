package at.pegelhub.station.application;

import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.station.domain.Station;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.station.persistence.StationRepository;
import at.pegelhub.stationowner.application.CreateStationOwnerCommand;
import at.pegelhub.stationowner.application.StationOwnerService;
import at.pegelhub.stationowner.domain.StationOwner;
import at.pegelhub.stationowner.domain.StationOwnerId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class StationServiceImplTest {

    private static final StationId STATION_ID = new StationId(UUID.fromString("50bce8fb-ae1d-486f-af6f-641843d6f401"));
    private static final StationOwnerId OWNER_ID = new StationOwnerId(UUID.fromString("1854fb48-2737-477a-9a4a-73c904ca7152"));
    private static final StationOwner OWNER = new StationOwner(OWNER_ID, "Hydro Org", "HO", null);

    private final InMemoryStationRepository repository = new InMemoryStationRepository();
    private final InMemoryStationOwnerService stationOwners = new InMemoryStationOwnerService();
    private final StationService service = new StationServiceImpl(repository, stationOwners);

    @Test
    void createsStationForExistingOwner() {
        stationOwners.owners.add(OWNER);

        var station = service.create(new CreateStationCommand(OWNER_ID, "1001", "Kienstock", "Danube", "Wachau"));

        assertThat(station.id()).isNotNull();
        assertThat(station.ownerId()).isEqualTo(OWNER_ID);
        assertThat(station.stationNumber()).isEqualTo("1001");
        assertThat(repository.saved).containsExactly(station);
    }

    @Test
    void refusesStationForMissingOwner() {
        assertThrows(NotFoundException.class,
                () -> service.create(new CreateStationCommand(OWNER_ID, "1001", "Kienstock", "Danube", null)));

        assertThat(repository.saved).isEmpty();
    }

    @Test
    void getsStationById() {
        var station = new Station(STATION_ID, OWNER_ID, "1001", "Kienstock", "Danube", null);
        repository.saved.add(station);

        assertThat(service.get(STATION_ID)).isEqualTo(station);
    }

    @Test
    void throwsNotFoundForMissingStation() {
        assertThrows(NotFoundException.class, () -> service.get(STATION_ID));
    }

    @Test
    void listsStations() {
        var station = new Station(STATION_ID, OWNER_ID, "1001", "Kienstock", "Danube", null);
        repository.saved.add(station);

        assertThat(service.list()).containsExactly(station);
    }

    private static final class InMemoryStationRepository implements StationRepository {

        private final List<Station> saved = new ArrayList<>();

        @Override
        public Station save(Station station) {
            saved.add(station);
            return station;
        }

        @Override
        public Optional<Station> findById(StationId id) {
            return saved.stream()
                    .filter(station -> station.id().equals(id))
                    .findFirst();
        }

        @Override
        public List<Station> findAll() {
            return List.copyOf(saved);
        }
    }

    private static final class InMemoryStationOwnerService implements StationOwnerService {

        private final List<StationOwner> owners = new ArrayList<>();

        @Override
        public StationOwner create(CreateStationOwnerCommand command) {
            throw new UnsupportedOperationException("Not needed by this test");
        }

        @Override
        public StationOwner get(StationOwnerId id) {
            return owners.stream()
                    .filter(owner -> owner.id().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("Station owner not found: " + id.value()));
        }

        @Override
        public List<StationOwner> list() {
            return List.copyOf(owners);
        }
    }
}
