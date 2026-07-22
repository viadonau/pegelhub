package at.pegelhub.measuringpoint.application;

import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.measuringpoint.persistence.MeasuringPointRepository;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.station.application.CreateStationCommand;
import at.pegelhub.station.application.StationService;
import at.pegelhub.station.domain.Station;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.stationowner.domain.StationOwnerId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class MeasuringPointServiceImplTest {

    private static final StationId STATION_ID = new StationId(
            UUID.fromString("dc59ba96-5ebc-404a-b220-95b18e8272b8"));
    private static final StationId OTHER_STATION_ID = new StationId(
            UUID.fromString("158c87fa-dc87-44f0-a92c-2fd93bc1020a"));
    private static final MeasuringPointId MEASURING_POINT_ID = new MeasuringPointId(
            UUID.fromString("2ff15221-acd5-49eb-a47d-e2df7206d034"));
    private static final Station STATION = station(STATION_ID, "1001");

    private final InMemoryMeasuringPointRepository repository = new InMemoryMeasuringPointRepository();
    private final InMemoryStationService stations = new InMemoryStationService();
    private final MeasuringPointService service = new MeasuringPointServiceImpl(repository, stations);

    @Test
    void createsMeasuringPointForExistingStation() {
        stations.stations.add(STATION);

        var measuringPoint = service.create(command(STATION_ID));

        assertThat(measuringPoint.id()).isNotNull();
        assertThat(measuringPoint.stationId()).isEqualTo(STATION_ID);
        assertThat(measuringPoint.name()).isEqualTo("Main gauge");
        assertThat(measuringPoint.referenceYear()).isEqualTo(2010);
        assertThat(measuringPoint.bank()).isEqualTo("R");
        assertThat(repository.saved).containsExactly(measuringPoint);
    }

    @Test
    void refusesMeasuringPointForMissingStation() {
        assertThrows(NotFoundException.class, () -> service.create(command(STATION_ID)));

        assertThat(repository.saved).isEmpty();
    }

    @Test
    void getsMeasuringPointById() {
        var measuringPoint = measuringPoint(MEASURING_POINT_ID, STATION_ID);
        repository.saved.add(measuringPoint);

        assertThat(service.get(MEASURING_POINT_ID)).isEqualTo(measuringPoint);
    }

    @Test
    void throwsNotFoundForMissingMeasuringPoint() {
        assertThrows(NotFoundException.class, () -> service.get(MEASURING_POINT_ID));
    }

    @Test
    void listsMeasuringPoints() {
        var measuringPoint = measuringPoint(MEASURING_POINT_ID, STATION_ID);
        repository.saved.add(measuringPoint);

        assertThat(service.list()).containsExactly(measuringPoint);
    }

    @Test
    void listsMeasuringPointsForExistingStation() {
        stations.stations.add(STATION);
        var matching = measuringPoint(MEASURING_POINT_ID, STATION_ID);
        var other = measuringPoint(
                new MeasuringPointId(UUID.fromString("61c14206-3bd3-4471-9571-e3d21dce5e58")),
                OTHER_STATION_ID);
        repository.saved.addAll(List.of(matching, other));

        assertThat(service.listForStation(STATION_ID)).containsExactly(matching);
    }

    @Test
    void refusesStationFilteredListForMissingStation() {
        assertThrows(NotFoundException.class, () -> service.listForStation(STATION_ID));
    }

    private static CreateMeasuringPointCommand command(StationId stationId) {
        return new CreateMeasuringPointCommand(
                stationId,
                "Main gauge",
                120.0,
                2010,
                1921.34,
                "R",
                162.0,
                295.0,
                480.0,
                760.0);
    }

    private static MeasuringPoint measuringPoint(MeasuringPointId id, StationId stationId) {
        return new MeasuringPoint(
                id,
                stationId,
                "Main gauge",
                120.0,
                2010,
                1921.34,
                "R",
                162.0,
                295.0,
                480.0,
                760.0);
    }

    private static Station station(StationId id, String number) {
        return new Station(
                id,
                new StationOwnerId(UUID.fromString("931ba418-c4e1-44ad-82ce-28c54b66256b")),
                number,
                "Kienstock",
                "Danube",
                null);
    }

    private static final class InMemoryMeasuringPointRepository implements MeasuringPointRepository {

        private final List<MeasuringPoint> saved = new ArrayList<>();

        @Override
        public MeasuringPoint save(MeasuringPoint measuringPoint) {
            saved.add(measuringPoint);
            return measuringPoint;
        }

        @Override
        public Optional<MeasuringPoint> findById(MeasuringPointId id) {
            return saved.stream()
                    .filter(measuringPoint -> measuringPoint.id().equals(id))
                    .findFirst();
        }

        @Override
        public List<MeasuringPoint> findAll() {
            return List.copyOf(saved);
        }

        @Override
        public List<MeasuringPoint> findByStationId(StationId stationId) {
            return saved.stream()
                    .filter(measuringPoint -> measuringPoint.stationId().equals(stationId))
                    .toList();
        }
    }

    private static final class InMemoryStationService implements StationService {

        private final List<Station> stations = new ArrayList<>();

        @Override
        public Station create(CreateStationCommand command) {
            throw new UnsupportedOperationException("Not needed by this test");
        }

        @Override
        public Station get(StationId id) {
            return stations.stream()
                    .filter(station -> station.id().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("Station not found: " + id.value()));
        }

        @Override
        public List<Station> list() {
            return List.copyOf(stations);
        }
    }
}
