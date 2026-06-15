package at.pegelhub.timeseries.application;

import at.pegelhub.connector.application.ConnectorService;
import at.pegelhub.connector.application.CreateConnectorCommand;
import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.connector.domain.ConnectorStatus;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.station.application.CreateStationCommand;
import at.pegelhub.station.application.StationService;
import at.pegelhub.station.domain.Station;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.stationowner.domain.StationOwnerId;
import at.pegelhub.timeseries.domain.ExternalTimeSeriesCode;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import at.pegelhub.timeseries.domain.UnitCode;
import at.pegelhub.timeseries.persistence.TimeSeriesRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static at.pegelhub.testsupport.ExampleData.CONTACT;

final class TimeSeriesServiceImplTest {

    private static final StationId STATION_ID = new StationId(UUID.fromString("dc59ba96-5ebc-404a-b220-95b18e8272b8"));
    private static final StationId OTHER_STATION_ID = new StationId(UUID.fromString("158c87fa-dc87-44f0-a92c-2fd93bc1020a"));
    private static final ConnectorId SOURCE_CONNECTOR_ID = new ConnectorId(UUID.fromString("63b82393-3c4a-43fd-ae0c-f47ec13d0e75"));
    private static final TimeSeriesId TIME_SERIES_ID = new TimeSeriesId(UUID.fromString("2ff15221-acd5-49eb-a47d-e2df7206d034"));
    private static final Station STATION = new Station(
            STATION_ID,
            new StationOwnerId(UUID.fromString("931ba418-c4e1-44ad-82ce-28c54b66256b")),
            "1001",
            "Kienstock",
            "Danube",
            null);

    private final InMemoryTimeSeriesRepository repository = new InMemoryTimeSeriesRepository();
    private final InMemoryStationService stations = new InMemoryStationService();
    private final InMemoryConnectorService connectors = new InMemoryConnectorService();
    private final TimeSeriesService service = new TimeSeriesServiceImpl(repository, stations, connectors);

    @Test
    void createsTimeSeriesForExistingStation() {
        stations.stations.add(STATION);
        connectors.connectorIds.add(SOURCE_CONNECTOR_ID);

        var timeSeries = service.create(command(STATION_ID));

        assertThat(timeSeries.id()).isNotNull();
        assertThat(timeSeries.stationId()).isEqualTo(STATION_ID);
        assertThat(timeSeries.observedProperty()).isEqualTo(new ObservedPropertyCode("water-level"));
        assertThat(timeSeries.sourceConnectorId()).isEqualTo(SOURCE_CONNECTOR_ID);
        assertThat(repository.saved).containsExactly(timeSeries);
    }

    @Test
    void refusesTimeSeriesForMissingSourceConnector() {
        stations.stations.add(STATION);

        assertThrows(NotFoundException.class, () -> service.create(command(STATION_ID)));

        assertThat(repository.saved).isEmpty();
    }

    @Test
    void refusesTimeSeriesForMissingStation() {
        assertThrows(NotFoundException.class, () -> service.create(command(STATION_ID)));

        assertThat(repository.saved).isEmpty();
    }

    @Test
    void getsTimeSeriesById() {
        var timeSeries = timeSeries(TIME_SERIES_ID, STATION_ID);
        repository.saved.add(timeSeries);

        assertThat(service.get(TIME_SERIES_ID)).isEqualTo(timeSeries);
    }

    @Test
    void throwsNotFoundForMissingTimeSeries() {
        assertThrows(NotFoundException.class, () -> service.get(TIME_SERIES_ID));
    }

    @Test
    void listsTimeSeries() {
        var timeSeries = timeSeries(TIME_SERIES_ID, STATION_ID);
        repository.saved.add(timeSeries);

        assertThat(service.list()).containsExactly(timeSeries);
    }

    @Test
    void listsTimeSeriesForExistingStation() {
        stations.stations.add(STATION);
        var matching = timeSeries(TIME_SERIES_ID, STATION_ID);
        var other = timeSeries(new TimeSeriesId(UUID.fromString("b7edee01-02fb-4b61-bd83-bd86c725f733")), OTHER_STATION_ID);
        repository.saved.addAll(List.of(matching, other));

        assertThat(service.listForStation(STATION_ID)).containsExactly(matching);
    }

    @Test
    void refusesStationFilteredListForMissingStation() {
        assertThrows(NotFoundException.class, () -> service.listForStation(STATION_ID));
    }

    private static CreateTimeSeriesCommand command(StationId stationId) {
        return new CreateTimeSeriesCommand(
                stationId,
                new ObservedPropertyCode("water-level"),
                new UnitCode("cm"),
                120.0,
                new ExternalTimeSeriesCode("main-stage"),
                SOURCE_CONNECTOR_ID);
    }

    private static TimeSeries timeSeries(TimeSeriesId id, StationId stationId) {
        return new TimeSeries(
                id,
                stationId,
                new ObservedPropertyCode("water-level"),
                new UnitCode("cm"),
                120.0,
                new ExternalTimeSeriesCode("main-stage"),
                SOURCE_CONNECTOR_ID);
    }

    private static final class InMemoryTimeSeriesRepository implements TimeSeriesRepository {

        private final List<TimeSeries> saved = new ArrayList<>();

        @Override
        public TimeSeries save(TimeSeries timeSeries) {
            saved.add(timeSeries);
            return timeSeries;
        }

        @Override
        public Optional<TimeSeries> findById(TimeSeriesId id) {
            return saved.stream()
                    .filter(timeSeries -> timeSeries.id().equals(id))
                    .findFirst();
        }

        @Override
        public List<TimeSeries> findAll() {
            return List.copyOf(saved);
        }

        @Override
        public List<TimeSeries> findByStationId(StationId stationId) {
            return saved.stream()
                    .filter(timeSeries -> timeSeries.stationId().equals(stationId))
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

    private static final class InMemoryConnectorService implements ConnectorService {

        private final List<ConnectorId> connectorIds = new ArrayList<>();

        @Override
        public Connector create(CreateConnectorCommand command) {
            throw new UnsupportedOperationException("Not needed by this test");
        }

        @Override
        public Connector register(String keycloakClientId, ConnectorStatus status, CreateConnectorCommand command) {
            throw new UnsupportedOperationException("Not needed by this test");
        }

        @Override
        public Connector get(ConnectorId id) {
            if (!connectorIds.contains(id)) {
                throw new NotFoundException("Connector not found: " + id.value());
            }
            return new Connector(id, "test", CONTACT, "type", "1.0", "1.0", "def", CONTACT, CONTACT, CONTACT, "", null, ConnectorStatus.ACTIVE);
        }

        @Override
        public List<Connector> list() {
            throw new UnsupportedOperationException("Not needed by this test");
        }

        @Override
        public void delete(ConnectorId id) {
            throw new UnsupportedOperationException("Not needed by this test");
        }
    }
}
