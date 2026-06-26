package at.pegelhub.access.application;

import at.pegelhub.access.domain.AccessGrant;
import at.pegelhub.access.domain.AccessGrantId;
import at.pegelhub.access.domain.AccessPermission;
import at.pegelhub.access.domain.AccessResourceRef;
import at.pegelhub.access.persistence.AccessGrantRepository;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.station.domain.StationId;
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

final class AccessAuthorizationServiceImplTest {

    private static final ConnectorId CONNECTOR_ID = new ConnectorId(UUID.fromString("72f95592-cfa2-442f-b819-e8d3be4c7b66"));
    private static final ConnectorId OTHER_CONNECTOR_ID = new ConnectorId(UUID.fromString("9e81d0bb-08fd-482a-8781-df58a85800d9"));
    private static final StationId STATION_ID = new StationId(UUID.fromString("100fc496-3021-4fdc-8545-8e3dadac08b0"));
    private static final StationId OTHER_STATION_ID = new StationId(UUID.fromString("4ac4756d-b005-4527-bb3d-e77ef44c6453"));
    private static final TimeSeriesId TIME_SERIES_ID = new TimeSeriesId(UUID.fromString("7eef12a3-9df6-4c61-837c-6ec374d0903c"));
    private static final TimeSeriesId OTHER_TIME_SERIES_ID = new TimeSeriesId(UUID.fromString("75c0eb28-3f60-4fee-9d98-d5bd2ed03637"));

    private final InMemoryAccessGrantRepository grants = new InMemoryAccessGrantRepository();
    private final InMemoryTimeSeriesService timeSeries = new InMemoryTimeSeriesService();
    private final AccessAuthorizationService service = new AccessAuthorizationServiceImpl(grants, timeSeries);

    @Test
    void allowsDirectMatchingGrant() {
        grants.saved.add(grant(CONNECTOR_ID, AccessResourceRef.timeSeries(TIME_SERIES_ID), AccessPermission.WRITE));

        assertThat(service.isAllowed(
                CONNECTOR_ID,
                AccessResourceRef.timeSeries(TIME_SERIES_ID),
                AccessPermission.WRITE)).isTrue();
        assertThat(timeSeries.getCount).isZero();
    }

    @Test
    void deniesWhenConnectorDoesNotMatch() {
        grants.saved.add(grant(OTHER_CONNECTOR_ID, AccessResourceRef.timeSeries(TIME_SERIES_ID), AccessPermission.WRITE));

        assertThat(service.isAllowed(
                CONNECTOR_ID,
                AccessResourceRef.timeSeries(TIME_SERIES_ID),
                AccessPermission.WRITE)).isFalse();
    }

    @Test
    void deniesWhenPermissionDoesNotMatch() {
        grants.saved.add(grant(CONNECTOR_ID, AccessResourceRef.timeSeries(TIME_SERIES_ID), AccessPermission.READ));

        assertThat(service.isAllowed(
                CONNECTOR_ID,
                AccessResourceRef.timeSeries(TIME_SERIES_ID),
                AccessPermission.WRITE)).isFalse();
    }

    @Test
    void stationGrantAllowsExistingTimeSeriesAtThatStation() {
        timeSeries.saved.add(timeSeries(TIME_SERIES_ID, STATION_ID));
        grants.saved.add(grant(CONNECTOR_ID, AccessResourceRef.station(STATION_ID), AccessPermission.READ));

        assertThat(service.isAllowed(
                CONNECTOR_ID,
                AccessResourceRef.timeSeries(TIME_SERIES_ID),
                AccessPermission.READ)).isTrue();
    }

    @Test
    void stationCascadeLoadsRequestedTimeSeriesOnlyOnce() {
        timeSeries.saved.add(timeSeries(TIME_SERIES_ID, STATION_ID));
        grants.saved.add(grant(CONNECTOR_ID, AccessResourceRef.station(OTHER_STATION_ID), AccessPermission.READ));
        grants.saved.add(grant(CONNECTOR_ID, AccessResourceRef.station(STATION_ID), AccessPermission.READ));

        assertThat(service.isAllowed(
                CONNECTOR_ID,
                AccessResourceRef.timeSeries(TIME_SERIES_ID),
                AccessPermission.READ)).isTrue();
        assertThat(timeSeries.getCount).isEqualTo(1);
    }

    @Test
    void stationGrantDoesNotAllowTimeSeriesAtOtherStation() {
        timeSeries.saved.add(timeSeries(TIME_SERIES_ID, OTHER_STATION_ID));
        grants.saved.add(grant(CONNECTOR_ID, AccessResourceRef.station(STATION_ID), AccessPermission.READ));

        assertThat(service.isAllowed(
                CONNECTOR_ID,
                AccessResourceRef.timeSeries(TIME_SERIES_ID),
                AccessPermission.READ)).isFalse();
    }

    @Test
    void stationReadGrantDoesNotAllowWritingTimeSeriesAtThatStation() {
        timeSeries.saved.add(timeSeries(TIME_SERIES_ID, STATION_ID));
        grants.saved.add(grant(CONNECTOR_ID, AccessResourceRef.station(STATION_ID), AccessPermission.READ));

        assertThat(service.isAllowed(
                CONNECTOR_ID,
                AccessResourceRef.timeSeries(TIME_SERIES_ID),
                AccessPermission.WRITE)).isFalse();
    }

    private static AccessGrant grant(ConnectorId connectorId, AccessResourceRef resource, AccessPermission permission) {
        return new AccessGrant(
                new AccessGrantId(UUID.randomUUID()),
                connectorId,
                resource,
                permission);
    }

    private static TimeSeries timeSeries(TimeSeriesId id, StationId stationId) {
        return new TimeSeries(
                id,
                stationId,
                new ObservedPropertyCode("water-level"),
                new UnitCode("cm"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
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

    private static final class InMemoryTimeSeriesService implements TimeSeriesService {

        private final List<TimeSeries> saved = new ArrayList<>();
        private int getCount;

        @Override
        public TimeSeries create(CreateTimeSeriesCommand command) {
            throw new UnsupportedOperationException("Not needed by this test");
        }

        @Override
        public TimeSeries get(TimeSeriesId id) {
            getCount++;
            return saved.stream()
                    .filter(series -> series.id().equals(id))
                    .findFirst()
                    .orElseThrow();
        }

        @Override
        public List<TimeSeries> list() {
            return List.copyOf(saved);
        }

        @Override
        public List<TimeSeries> listForStation(StationId stationId) {
            return saved.stream()
                    .filter(series -> series.stationId().equals(stationId))
                    .toList();
        }
    }
}
