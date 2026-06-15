package at.pegelhub.measurement.application;

import at.pegelhub.access.application.AccessAuthorizationService;
import at.pegelhub.access.domain.AccessPermission;
import at.pegelhub.access.domain.AccessResourceRef;
import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.connector.domain.ConnectorStatus;
import at.pegelhub.connector.persistence.ConnectorRepository;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.measurement.domain.WriteMeasurement;
import at.pegelhub.measurement.domain.WriteMeasurements;
import at.pegelhub.measurement.persistence.MeasurementRepository;
import at.pegelhub.security.CurrentActor;
import at.pegelhub.security.PegelHubAuthority;
import at.pegelhub.security.PegelHubActor;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.timeseries.application.TimeSeriesService;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import at.pegelhub.timeseries.domain.UnitCode;
import at.pegelhub.station.domain.StationId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static at.pegelhub.testsupport.ExampleData.CONTACT;
import static at.pegelhub.testsupport.ExampleData.MEASUREMENT;
import static at.pegelhub.testsupport.ExampleData.MEASUREMENT_AVERAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class MeasurementServiceImplTest {

    private MeasurementServiceImpl measurementService;

    private static final UUID CONNECTOR_UUID = UUID.fromString("0d9a3c87-b41a-4663-af0a-f6ec5e6a91cf");
    private static final TimeSeriesId TIME_SERIES_ID = new TimeSeriesId(UUID.fromString("8ce8c5b6-f093-4d46-b770-7239cdfa3d76"));
    private static final Instant OBSERVED_AT = Instant.parse("2026-04-25T10:15:30Z");
    private static final ConnectorRepository CONNECTOR_REPOSITORY = mock(ConnectorRepository.class);
    private static final MeasurementRepository MEASUREMENT_REPOSITORY = mock(MeasurementRepository.class);
    private static final CurrentActor CURRENT_ACTOR = mock(CurrentActor.class);
    private static final TimeSeriesService TIME_SERIES_SERVICE = mock(TimeSeriesService.class);
    private static final AccessAuthorizationService ACCESS_AUTHORIZATION_SERVICE = mock(AccessAuthorizationService.class);

    @BeforeEach
    void prepare() {
        measurementService = new MeasurementServiceImpl(
                CONNECTOR_REPOSITORY,
                MEASUREMENT_REPOSITORY,
                CURRENT_ACTOR,
                TIME_SERIES_SERVICE,
                ACCESS_AUTHORIZATION_SERVICE);
        reset(CONNECTOR_REPOSITORY, MEASUREMENT_REPOSITORY, CURRENT_ACTOR, TIME_SERIES_SERVICE, ACCESS_AUTHORIZATION_SERVICE);
        when(CURRENT_ACTOR.get()).thenReturn(new PegelHubActor("subject", "local-connector-example", Set.of()));
        when(TIME_SERIES_SERVICE.get(TIME_SERIES_ID)).thenReturn(timeSeries(new ConnectorId(CONNECTOR_UUID)));
    }

    @Test
    void constructorWithNullArgsThrowsNpe() {
        assertThrows(NullPointerException.class, () -> new MeasurementServiceImpl(null, MEASUREMENT_REPOSITORY, CURRENT_ACTOR, TIME_SERIES_SERVICE, ACCESS_AUTHORIZATION_SERVICE));
        assertThrows(NullPointerException.class, () -> new MeasurementServiceImpl(CONNECTOR_REPOSITORY, null, CURRENT_ACTOR, TIME_SERIES_SERVICE, ACCESS_AUTHORIZATION_SERVICE));
        assertThrows(NullPointerException.class, () -> new MeasurementServiceImpl(CONNECTOR_REPOSITORY, MEASUREMENT_REPOSITORY, null, TIME_SERIES_SERVICE, ACCESS_AUTHORIZATION_SERVICE));
        assertThrows(NullPointerException.class, () -> new MeasurementServiceImpl(CONNECTOR_REPOSITORY, MEASUREMENT_REPOSITORY, CURRENT_ACTOR, null, ACCESS_AUTHORIZATION_SERVICE));
        assertThrows(NullPointerException.class, () -> new MeasurementServiceImpl(CONNECTOR_REPOSITORY, MEASUREMENT_REPOSITORY, CURRENT_ACTOR, TIME_SERIES_SERVICE, null));
    }

    @Test
    void writeMeasurementsStoresAuthorizedTimeSeriesMeasurements() {
        when(CONNECTOR_REPOSITORY.findByKeycloakClientId("local-connector-example"))
                .thenReturn(Optional.of(connector(ConnectorStatus.ACTIVE)));
        when(ACCESS_AUTHORIZATION_SERVICE.isAllowed(
                eq(new ConnectorId(CONNECTOR_UUID)),
                eq(AccessResourceRef.timeSeries(TIME_SERIES_ID)),
                eq(AccessPermission.WRITE)))
                .thenReturn(true);

        measurementService.writeMeasurements(new WriteMeasurements(List.of(new WriteMeasurement(
                TIME_SERIES_ID,
                OBSERVED_AT,
                10.5))));

        verify(TIME_SERIES_SERVICE).get(TIME_SERIES_ID);
        verify(MEASUREMENT_REPOSITORY).storeMeasurements(argThat(measurements -> {
            Measurement stored = measurements.getFirst();
            return measurements.size() == 1
                    && TIME_SERIES_ID.equals(stored.timeSeriesId())
                    && OBSERVED_AT.equals(stored.observedAt())
                    && stored.value() == 10.5
                    && new ConnectorId(CONNECTOR_UUID).equals(stored.submittedByConnectorId());
        }));
    }

    @Test
    void writeMeasurementsThrowsNotFoundWhenConnectorIsUnknown() {
        when(CONNECTOR_REPOSITORY.findByKeycloakClientId("local-connector-example")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> measurementService.writeMeasurements(new WriteMeasurements(List.of(
                new WriteMeasurement(TIME_SERIES_ID, OBSERVED_AT, 10.5)
        ))));
    }

    @Test
    void writeMeasurementsThrowsAccessDeniedWhenConnectorIsSuspended() {
        when(CONNECTOR_REPOSITORY.findByKeycloakClientId("local-connector-example"))
                .thenReturn(Optional.of(connector(ConnectorStatus.SUSPENDED)));

        assertThrows(AccessDeniedException.class, () -> measurementService.writeMeasurements(new WriteMeasurements(List.of(
                new WriteMeasurement(TIME_SERIES_ID, OBSERVED_AT, 10.5)
        ))));
    }

    @Test
    void writeMeasurementsThrowsAccessDeniedWhenConnectorHasNoGrant() {
        when(CONNECTOR_REPOSITORY.findByKeycloakClientId("local-connector-example"))
                .thenReturn(Optional.of(connector(ConnectorStatus.ACTIVE)));
        when(ACCESS_AUTHORIZATION_SERVICE.isAllowed(
                eq(new ConnectorId(CONNECTOR_UUID)),
                eq(AccessResourceRef.timeSeries(TIME_SERIES_ID)),
                eq(AccessPermission.WRITE)))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> measurementService.writeMeasurements(new WriteMeasurements(List.of(
                new WriteMeasurement(TIME_SERIES_ID, OBSERVED_AT, 10.5)
        ))));
    }

    @Test
    void writeMeasurementsThrowsAccessDeniedWhenConnectorIsNotTimeSeriesSource() {
        when(CONNECTOR_REPOSITORY.findByKeycloakClientId("local-connector-example"))
                .thenReturn(Optional.of(connector(ConnectorStatus.ACTIVE)));
        when(TIME_SERIES_SERVICE.get(TIME_SERIES_ID)).thenReturn(timeSeries(
                new ConnectorId(UUID.fromString("cbe77f6f-4411-4bd0-a099-a5437a4105b2"))));

        assertThrows(AccessDeniedException.class, () -> measurementService.writeMeasurements(new WriteMeasurements(List.of(
                new WriteMeasurement(TIME_SERIES_ID, OBSERVED_AT, 10.5)
        ))));
    }

    @Test
    void getByTimeSeriesAndRangeReturnsMeasurementsForExistingTimeSeries() {
        allowReadAccess();
        when(MEASUREMENT_REPOSITORY.getByTimeSeriesIdAndRange(TIME_SERIES_ID, "72d")).thenReturn(List.of(MEASUREMENT));

        List<Measurement> result = measurementService.getByTimeSeriesAndRange(TIME_SERIES_ID, "72d");

        assertEquals(1, result.size());
        assertEquals(MEASUREMENT, result.getFirst());
        verify(TIME_SERIES_SERVICE).get(TIME_SERIES_ID);
    }

    @Test
    void getLatestByTimeSeriesDelegatesToRepository() {
        allowReadAccess();
        when(MEASUREMENT_REPOSITORY.getLatestByTimeSeriesId(TIME_SERIES_ID)).thenReturn(MEASUREMENT);

        Measurement result = measurementService.getLatestByTimeSeries(TIME_SERIES_ID);

        assertEquals(MEASUREMENT, result);
        verify(TIME_SERIES_SERVICE).get(TIME_SERIES_ID);
    }

    @Test
    void getAverageByTimeSeriesAndRangeReturnsAverageMeasurement() {
        allowReadAccess();
        when(MEASUREMENT_REPOSITORY.getAverageByTimeSeriesIdAndRange(TIME_SERIES_ID, "72d")).thenReturn(MEASUREMENT_AVERAGE);

        var result = measurementService.getAverageByTimeSeriesAndRange(TIME_SERIES_ID, "72d");

        assertEquals(MEASUREMENT_AVERAGE, result);
        verify(TIME_SERIES_SERVICE).get(TIME_SERIES_ID);
    }

    @Test
    void getByTimeSeriesAndRangeThrowsAccessDeniedWhenConnectorHasNoReadGrant() {
        when(CONNECTOR_REPOSITORY.findByKeycloakClientId("local-connector-example"))
                .thenReturn(Optional.of(connector(ConnectorStatus.ACTIVE)));
        when(ACCESS_AUTHORIZATION_SERVICE.isAllowed(
                eq(new ConnectorId(CONNECTOR_UUID)),
                eq(AccessResourceRef.timeSeries(TIME_SERIES_ID)),
                eq(AccessPermission.READ)))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> measurementService.getByTimeSeriesAndRange(TIME_SERIES_ID, "72d"));
    }

    @Test
    void getLatestByTimeSeriesAllowsSystemAdminWithoutAccessGrant() {
        when(CURRENT_ACTOR.get()).thenReturn(new PegelHubActor(
                "subject",
                "operator-client",
                Set.of(PegelHubAuthority.SYSTEM_ADMIN)));
        when(MEASUREMENT_REPOSITORY.getLatestByTimeSeriesId(TIME_SERIES_ID)).thenReturn(MEASUREMENT);

        Measurement result = measurementService.getLatestByTimeSeries(TIME_SERIES_ID);

        assertEquals(MEASUREMENT, result);
        verify(TIME_SERIES_SERVICE).get(TIME_SERIES_ID);
    }

    @Test
    void getSystemTimeDelegatesToRepository() {
        Instant ts = Instant.parse("2026-01-02T03:04:05Z");
        when(MEASUREMENT_REPOSITORY.getSystemTime()).thenReturn(ts);

        Instant result = measurementService.getSystemTime();

        assertEquals(ts, result);
    }

    private static Connector connector(ConnectorStatus status) {
        return new Connector(
                new ConnectorId(CONNECTOR_UUID),
                "test", CONTACT, "type", "1.0", "1.0", "def",
                CONTACT, CONTACT, CONTACT, "",
                "local-connector-example", status);
    }

    private static void allowReadAccess() {
        when(CONNECTOR_REPOSITORY.findByKeycloakClientId("local-connector-example"))
                .thenReturn(Optional.of(connector(ConnectorStatus.ACTIVE)));
        when(ACCESS_AUTHORIZATION_SERVICE.isAllowed(
                eq(new ConnectorId(CONNECTOR_UUID)),
                eq(AccessResourceRef.timeSeries(TIME_SERIES_ID)),
                eq(AccessPermission.READ)))
                .thenReturn(true);
    }

    private static TimeSeries timeSeries(ConnectorId sourceConnectorId) {
        return new TimeSeries(
                TIME_SERIES_ID,
                new StationId(UUID.fromString("7f65e3b7-97b4-4016-83a3-77f51332dc01")),
                new ObservedPropertyCode("water-level"),
                new UnitCode("cm"),
                null,
                null,
                sourceConnectorId);
    }
}
