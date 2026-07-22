package at.pegelhub.timeseries.application;

import at.pegelhub.connector.application.ConnectorService;
import at.pegelhub.connector.application.CreateConnectorCommand;
import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.connector.domain.ConnectorStatus;
import at.pegelhub.measuringpoint.application.CreateMeasuringPointCommand;
import at.pegelhub.measuringpoint.application.MeasuringPointService;
import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static at.pegelhub.testsupport.ExampleData.CONTACT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class TimeSeriesServiceImplTest {

    private static final StationId STATION_ID = new StationId(
            UUID.fromString("dc59ba96-5ebc-404a-b220-95b18e8272b8"));
    private static final StationId OTHER_STATION_ID = new StationId(
            UUID.fromString("158c87fa-dc87-44f0-a92c-2fd93bc1020a"));
    private static final MeasuringPointId MEASURING_POINT_ID = new MeasuringPointId(
            UUID.fromString("857d2183-7481-4a4f-a9bd-b5496093965e"));
    private static final MeasuringPointId OTHER_MEASURING_POINT_ID = new MeasuringPointId(
            UUID.fromString("b2cebb6d-4660-4d75-bc08-0aacde669c2d"));
    private static final ConnectorId SOURCE_CONNECTOR_ID = new ConnectorId(
            UUID.fromString("63b82393-3c4a-43fd-ae0c-f47ec13d0e75"));
    private static final TimeSeriesId TIME_SERIES_ID = new TimeSeriesId(
            UUID.fromString("2ff15221-acd5-49eb-a47d-e2df7206d034"));
    private static final Station STATION = station(STATION_ID, "1001");
    private static final MeasuringPoint MEASURING_POINT = measuringPoint(MEASURING_POINT_ID, STATION_ID);

    private final InMemoryTimeSeriesRepository repository = new InMemoryTimeSeriesRepository();
    private final InMemoryMeasuringPointService measuringPoints = new InMemoryMeasuringPointService();
    private final InMemoryStationService stations = new InMemoryStationService();
    private final InMemoryConnectorService connectors = new InMemoryConnectorService();
    private final TimeSeriesService service = new TimeSeriesServiceImpl(
            repository,
            measuringPoints,
            stations,
            connectors);

    @Test
    void createsTimeSeriesForExistingMeasuringPoint() {
        measuringPoints.measuringPoints.add(MEASURING_POINT);
        connectors.connectorIds.add(SOURCE_CONNECTOR_ID);

        var timeSeries = service.create(command(MEASURING_POINT_ID));

        assertThat(timeSeries.id()).isNotNull();
        assertThat(timeSeries.measuringPointId()).isEqualTo(MEASURING_POINT_ID);
        assertThat(timeSeries.observedProperty()).isEqualTo(new ObservedPropertyCode("water-level"));
        assertThat(timeSeries.sourceConnectorId()).isEqualTo(SOURCE_CONNECTOR_ID);
        assertThat(repository.saved).containsExactly(timeSeries);
    }

    @Test
    void refusesTimeSeriesForMissingSourceConnector() {
        measuringPoints.measuringPoints.add(MEASURING_POINT);

        assertThrows(NotFoundException.class, () -> service.create(command(MEASURING_POINT_ID)));

        assertThat(repository.saved).isEmpty();
    }

    @Test
    void refusesTimeSeriesForMissingMeasuringPoint() {
        connectors.connectorIds.add(SOURCE_CONNECTOR_ID);

        assertThrows(NotFoundException.class, () -> service.create(command(MEASURING_POINT_ID)));

        assertThat(repository.saved).isEmpty();
    }

    @Test
    void getsTimeSeriesById() {
        var timeSeries = timeSeries(TIME_SERIES_ID, MEASURING_POINT_ID);
        repository.saved.add(timeSeries);

        assertThat(service.get(TIME_SERIES_ID)).isEqualTo(timeSeries);
    }

    @Test
    void throwsNotFoundForMissingTimeSeries() {
        assertThrows(NotFoundException.class, () -> service.get(TIME_SERIES_ID));
    }

    @Test
    void listsTimeSeries() {
        var timeSeries = timeSeries(TIME_SERIES_ID, MEASURING_POINT_ID);
        repository.saved.add(timeSeries);

        assertThat(service.list()).containsExactly(timeSeries);
    }

    @Test
    void listsTimeSeriesForExistingMeasuringPoint() {
        measuringPoints.measuringPoints.add(MEASURING_POINT);
        var matching = timeSeries(TIME_SERIES_ID, MEASURING_POINT_ID);
        var other = timeSeries(
                new TimeSeriesId(UUID.fromString("b7edee01-02fb-4b61-bd83-bd86c725f733")),
                OTHER_MEASURING_POINT_ID);
        repository.saved.addAll(List.of(matching, other));

        assertThat(service.listForMeasuringPoint(MEASURING_POINT_ID)).containsExactly(matching);
    }

    @Test
    void refusesMeasuringPointFilteredListForMissingMeasuringPoint() {
        assertThrows(NotFoundException.class, () -> service.listForMeasuringPoint(MEASURING_POINT_ID));
    }

    @Test
    void listsTimeSeriesForExistingStationThroughMeasuringPoints() {
        stations.stations.add(STATION);
        repository.measuringPointStations.put(MEASURING_POINT_ID, STATION_ID);
        repository.measuringPointStations.put(OTHER_MEASURING_POINT_ID, OTHER_STATION_ID);
        var matching = timeSeries(TIME_SERIES_ID, MEASURING_POINT_ID);
        var other = timeSeries(
                new TimeSeriesId(UUID.fromString("b7edee01-02fb-4b61-bd83-bd86c725f733")),
                OTHER_MEASURING_POINT_ID);
        repository.saved.addAll(List.of(matching, other));

        assertThat(service.listForStation(STATION_ID)).containsExactly(matching);
    }

    @Test
    void refusesStationFilteredListForMissingStation() {
        assertThrows(NotFoundException.class, () -> service.listForStation(STATION_ID));
    }

    private static CreateTimeSeriesCommand command(MeasuringPointId measuringPointId) {
        return new CreateTimeSeriesCommand(
                measuringPointId,
                new ObservedPropertyCode("water-level"),
                new UnitCode("cm"),
                new ExternalTimeSeriesCode("main-stage"),
                SOURCE_CONNECTOR_ID);
    }

    private static TimeSeries timeSeries(TimeSeriesId id, MeasuringPointId measuringPointId) {
        return new TimeSeries(
                id,
                measuringPointId,
                new ObservedPropertyCode("water-level"),
                new UnitCode("cm"),
                new ExternalTimeSeriesCode("main-stage"),
                SOURCE_CONNECTOR_ID);
    }

    private static MeasuringPoint measuringPoint(MeasuringPointId id, StationId stationId) {
        return new MeasuringPoint(id, stationId, "Main gauge", null, null, null, null, null, null, null, null);
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

    private static final class InMemoryTimeSeriesRepository implements TimeSeriesRepository {

        private final List<TimeSeries> saved = new ArrayList<>();
        private final Map<MeasuringPointId, StationId> measuringPointStations = new HashMap<>();

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
        public List<TimeSeries> findByMeasuringPointId(MeasuringPointId measuringPointId) {
            return saved.stream()
                    .filter(timeSeries -> timeSeries.measuringPointId().equals(measuringPointId))
                    .toList();
        }

        @Override
        public List<TimeSeries> findByStationId(StationId stationId) {
            return saved.stream()
                    .filter(timeSeries -> stationId.equals(measuringPointStations.get(timeSeries.measuringPointId())))
                    .toList();
        }
    }

    private static final class InMemoryMeasuringPointService implements MeasuringPointService {

        private final List<MeasuringPoint> measuringPoints = new ArrayList<>();

        @Override
        public MeasuringPoint create(CreateMeasuringPointCommand command) {
            throw new UnsupportedOperationException("Not needed by this test");
        }

        @Override
        public MeasuringPoint get(MeasuringPointId id) {
            return measuringPoints.stream()
                    .filter(measuringPoint -> measuringPoint.id().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("Measuring point not found: " + id.value()));
        }

        @Override
        public List<MeasuringPoint> list() {
            return List.copyOf(measuringPoints);
        }

        @Override
        public List<MeasuringPoint> listForStation(StationId stationId) {
            return measuringPoints.stream()
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
            return new Connector(
                    id,
                    "test",
                    CONTACT,
                    "type",
                    "1.0",
                    "1.0",
                    "def",
                    CONTACT,
                    CONTACT,
                    CONTACT,
                    "",
                    null,
                    ConnectorStatus.ACTIVE);
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
