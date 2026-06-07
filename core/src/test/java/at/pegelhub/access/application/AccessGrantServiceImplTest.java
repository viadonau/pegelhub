package at.pegelhub.access.application;

import at.pegelhub.access.domain.AccessGrant;
import at.pegelhub.access.domain.AccessGrantId;
import at.pegelhub.access.domain.AccessPermission;
import at.pegelhub.access.domain.AccessResourceRef;
import at.pegelhub.access.persistence.AccessGrantRepository;
import at.pegelhub.connector.application.ConnectorService;
import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.connector.domain.ConnectorStatus;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.station.application.CreateStationCommand;
import at.pegelhub.station.application.StationService;
import at.pegelhub.station.domain.Station;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.stationowner.domain.StationOwnerId;
import at.pegelhub.timeseries.application.CreateTimeSeriesCommand;
import at.pegelhub.timeseries.application.TimeSeriesService;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import at.pegelhub.timeseries.domain.UnitCode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class AccessGrantServiceImplTest {

    private static final ConnectorId CONNECTOR_ID = new ConnectorId(UUID.fromString("a0c57d32-ce76-4e89-9ce9-caa3ee5e33f7"));
    private static final ConnectorId OTHER_CONNECTOR_ID = new ConnectorId(UUID.fromString("8c6c64dc-0e9f-4112-af69-18988aa1f022"));
    private static final StationId STATION_ID = new StationId(UUID.fromString("e08e267b-95bd-492f-93a3-3e50d727a1c4"));
    private static final TimeSeriesId TIME_SERIES_ID = new TimeSeriesId(UUID.fromString("65fc3eeb-b4dd-4749-bb67-8b1fc9f75b97"));
    private static final AccessGrantId GRANT_ID = new AccessGrantId(UUID.fromString("58bd0cc1-57b9-427f-9ee8-d432636183bf"));
    private static final Station STATION = new Station(
            STATION_ID,
            new StationOwnerId(UUID.fromString("a579a927-b7af-40dd-94e4-6419c1cfb704")),
            "1001",
            "Kienstock",
            "Danube",
            null);
    private static final TimeSeries TIME_SERIES = new TimeSeries(
            TIME_SERIES_ID,
            STATION_ID,
            new ObservedPropertyCode("water-level"),
            new UnitCode("cm"),
            null,
            null,
            null);

    private final InMemoryAccessGrantRepository repository = new InMemoryAccessGrantRepository();
    private final InMemoryConnectorService connectors = new InMemoryConnectorService();
    private final InMemoryStationService stations = new InMemoryStationService();
    private final InMemoryTimeSeriesService timeSeries = new InMemoryTimeSeriesService();
    private final AccessGrantService service = new AccessGrantServiceImpl(repository, connectors, stations, timeSeries);

    @Test
    void createsStationGrantForExistingConnectorAndStation() {
        connectors.connectorIds.add(CONNECTOR_ID);
        stations.stations.add(STATION);

        var grant = service.create(new CreateAccessGrantCommand(
                CONNECTOR_ID,
                AccessResourceRef.station(STATION_ID),
                AccessPermission.WRITE,
                null,
                null,
                true));

        assertThat(grant.connectorId()).isEqualTo(CONNECTOR_ID);
        assertThat(grant.resource()).isEqualTo(AccessResourceRef.station(STATION_ID));
        assertThat(grant.includeFutureTimeSeries()).isTrue();
        assertThat(repository.saved).containsExactly(grant);
    }

    @Test
    void createsTimeSeriesGrantForExistingConnectorAndTimeSeries() {
        connectors.connectorIds.add(CONNECTOR_ID);
        timeSeries.timeSeries.add(TIME_SERIES);

        var grant = service.create(new CreateAccessGrantCommand(
                CONNECTOR_ID,
                AccessResourceRef.timeSeries(TIME_SERIES_ID),
                AccessPermission.READ,
                null,
                null,
                false));

        assertThat(grant.resource()).isEqualTo(AccessResourceRef.timeSeries(TIME_SERIES_ID));
    }

    @Test
    void refusesGrantForMissingConnector() {
        stations.stations.add(STATION);

        assertThrows(NotFoundException.class, () -> service.create(new CreateAccessGrantCommand(
                CONNECTOR_ID,
                AccessResourceRef.station(STATION_ID),
                AccessPermission.READ,
                null,
                null,
                false)));

        assertThat(repository.saved).isEmpty();
    }

    @Test
    void refusesGrantForMissingStation() {
        connectors.connectorIds.add(CONNECTOR_ID);

        assertThrows(NotFoundException.class, () -> service.create(new CreateAccessGrantCommand(
                CONNECTOR_ID,
                AccessResourceRef.station(STATION_ID),
                AccessPermission.READ,
                null,
                null,
                false)));

        assertThat(repository.saved).isEmpty();
    }

    @Test
    void refusesGrantForMissingTimeSeries() {
        connectors.connectorIds.add(CONNECTOR_ID);

        assertThrows(NotFoundException.class, () -> service.create(new CreateAccessGrantCommand(
                CONNECTOR_ID,
                AccessResourceRef.timeSeries(TIME_SERIES_ID),
                AccessPermission.READ,
                null,
                null,
                false)));

        assertThat(repository.saved).isEmpty();
    }

    @Test
    void getsGrantById() {
        var grant = grant(GRANT_ID, CONNECTOR_ID);
        repository.saved.add(grant);

        assertThat(service.get(GRANT_ID)).isEqualTo(grant);
    }

    @Test
    void throwsNotFoundForMissingGrant() {
        assertThrows(NotFoundException.class, () -> service.get(GRANT_ID));
    }

    @Test
    void listsGrants() {
        var grant = grant(GRANT_ID, CONNECTOR_ID);
        repository.saved.add(grant);

        assertThat(service.list()).containsExactly(grant);
    }

    @Test
    void listsGrantsForExistingConnector() {
        connectors.connectorIds.add(CONNECTOR_ID);
        var matching = grant(GRANT_ID, CONNECTOR_ID);
        var other = grant(new AccessGrantId(UUID.fromString("7c2c9dd1-237f-4cdb-ae5e-b7a10c8d8bec")), OTHER_CONNECTOR_ID);
        repository.saved.addAll(List.of(matching, other));

        assertThat(service.listForConnector(CONNECTOR_ID)).containsExactly(matching);
    }

    @Test
    void refusesConnectorFilteredListForMissingConnector() {
        assertThrows(NotFoundException.class, () -> service.listForConnector(CONNECTOR_ID));
    }

    private static AccessGrant grant(AccessGrantId grantId, ConnectorId connectorId) {
        return new AccessGrant(
                grantId,
                connectorId,
                AccessResourceRef.station(STATION_ID),
                AccessPermission.READ,
                null,
                null,
                false);
    }

    private static final class InMemoryAccessGrantRepository implements AccessGrantRepository {

        private final List<AccessGrant> saved = new ArrayList<>();

        @Override
        public AccessGrant save(AccessGrant accessGrant) {
            saved.add(accessGrant);
            return accessGrant;
        }

        @Override
        public Optional<AccessGrant> findById(AccessGrantId id) {
            return saved.stream()
                    .filter(grant -> grant.id().equals(id))
                    .findFirst();
        }

        @Override
        public List<AccessGrant> findAll() {
            return List.copyOf(saved);
        }

        @Override
        public List<AccessGrant> findByConnectorId(ConnectorId connectorId) {
            return saved.stream()
                    .filter(grant -> grant.connectorId().equals(connectorId))
                    .toList();
        }
    }

    private static final class InMemoryConnectorService implements ConnectorService {

        private final List<ConnectorId> connectorIds = new ArrayList<>();

        @Override
        public Connector createConnector(Connector connector) {
            throw new UnsupportedOperationException("Not needed by this test");
        }

        @Override
        public Connector registerConnector(String keycloakClientId, ConnectorStatus status, Connector connector) {
            throw new UnsupportedOperationException("Not needed by this test");
        }

        @Override
        public Connector getConnectorById(UUID uuid) {
            if (!connectorIds.contains(new ConnectorId(uuid))) {
                throw new NotFoundException("Connector not found: " + uuid);
            }
            return new Connector();
        }

        @Override
        public List<Connector> getAllConnectors() {
            throw new UnsupportedOperationException("Not needed by this test");
        }

        @Override
        public void deleteConnector(UUID uuid) {
            throw new UnsupportedOperationException("Not needed by this test");
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

    private static final class InMemoryTimeSeriesService implements TimeSeriesService {

        private final List<TimeSeries> timeSeries = new ArrayList<>();

        @Override
        public TimeSeries create(CreateTimeSeriesCommand command) {
            throw new UnsupportedOperationException("Not needed by this test");
        }

        @Override
        public TimeSeries get(TimeSeriesId id) {
            return timeSeries.stream()
                    .filter(series -> series.id().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("Time series not found: " + id.value()));
        }

        @Override
        public List<TimeSeries> list() {
            return List.copyOf(timeSeries);
        }

        @Override
        public List<TimeSeries> listForStation(StationId stationId) {
            return timeSeries.stream()
                    .filter(series -> series.stationId().equals(stationId))
                    .toList();
        }
    }
}
