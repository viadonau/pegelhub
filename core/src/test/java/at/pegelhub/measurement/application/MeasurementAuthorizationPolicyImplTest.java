package at.pegelhub.measurement.application;

import at.pegelhub.access.application.AccessAuthorizationService;
import at.pegelhub.access.domain.AccessPermission;
import at.pegelhub.access.domain.AccessResourceRef;
import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.connector.domain.ConnectorStatus;
import at.pegelhub.connector.persistence.ConnectorRepository;
import at.pegelhub.security.CurrentActor;
import at.pegelhub.security.PegelHubActor;
import at.pegelhub.security.PegelHubActorType;
import at.pegelhub.security.PegelHubAuthority;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.timeseries.application.TimeSeriesService;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import at.pegelhub.timeseries.domain.UnitCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static at.pegelhub.security.PegelHubAuthority.MEASUREMENT_READ;
import static at.pegelhub.security.PegelHubAuthority.MEASUREMENT_WRITE;
import static at.pegelhub.security.PegelHubAuthority.SYSTEM_ADMIN;
import static at.pegelhub.testsupport.ExampleData.CONTACT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class MeasurementAuthorizationPolicyImplTest {

    private static final ConnectorId CONNECTOR_ID = new ConnectorId(UUID.fromString("0d9a3c87-b41a-4663-af0a-f6ec5e6a91cf"));
    private static final ConnectorId OTHER_CONNECTOR_ID = new ConnectorId(UUID.fromString("cbe77f6f-4411-4bd0-a099-a5437a4105b2"));
    private static final TimeSeriesId TIME_SERIES_ID = new TimeSeriesId(UUID.fromString("8ce8c5b6-f093-4d46-b770-7239cdfa3d76"));

    private final CurrentActor currentActor = mock(CurrentActor.class);
    private final ConnectorRepository connectorRepository = mock(ConnectorRepository.class);
    private final TimeSeriesService timeSeriesService = mock(TimeSeriesService.class);
    private final AccessAuthorizationService accessAuthorizationService = mock(AccessAuthorizationService.class);
    private final MeasurementAuthorizationPolicyImpl policy = new MeasurementAuthorizationPolicyImpl(
            currentActor,
            connectorRepository,
            timeSeriesService,
            accessAuthorizationService);

    @BeforeEach
    void prepare() {
        reset(currentActor, connectorRepository, timeSeriesService, accessAuthorizationService);
        when(timeSeriesService.get(TIME_SERIES_ID)).thenReturn(timeSeries(CONNECTOR_ID));
    }

    @Test
    void requireReadAllowsSystemAdminWithoutConnectorRegistration() {
        when(currentActor.get()).thenReturn(user(SYSTEM_ADMIN));

        policy.requireRead(TIME_SERIES_ID);

        verify(timeSeriesService).get(TIME_SERIES_ID);
        verify(connectorRepository, never()).findByKeycloakClientId("pegelhub-frontend");
    }

    @Test
    void requireReadAllowsOperatorUserWithoutConnectorRegistration() {
        when(currentActor.get()).thenReturn(user(MEASUREMENT_READ));

        policy.requireRead(TIME_SERIES_ID);

        verify(timeSeriesService).get(TIME_SERIES_ID);
        verify(connectorRepository, never()).findByKeycloakClientId("pegelhub-frontend");
    }

    @Test
    void requireReadDeniesUnregisteredClientWithMeasurementRead() {
        when(currentActor.get()).thenReturn(client(MEASUREMENT_READ));
        when(connectorRepository.findByKeycloakClientId("local-connector-example")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> policy.requireRead(TIME_SERIES_ID));
    }

    @Test
    void requireReadAllowsRegisteredActiveConnectorWithReadGrant() {
        when(currentActor.get()).thenReturn(client(MEASUREMENT_READ));
        when(connectorRepository.findByKeycloakClientId("local-connector-example"))
                .thenReturn(Optional.of(connector(ConnectorStatus.ACTIVE)));
        when(accessAuthorizationService.isAllowed(
                eq(CONNECTOR_ID),
                eq(AccessResourceRef.timeSeries(TIME_SERIES_ID)),
                eq(AccessPermission.READ)))
                .thenReturn(true);

        policy.requireRead(TIME_SERIES_ID);
    }

    @Test
    void requireReadDeniesConnectorWithoutReadGrant() {
        when(currentActor.get()).thenReturn(client(MEASUREMENT_READ));
        when(connectorRepository.findByKeycloakClientId("local-connector-example"))
                .thenReturn(Optional.of(connector(ConnectorStatus.ACTIVE)));
        when(accessAuthorizationService.isAllowed(
                eq(CONNECTOR_ID),
                eq(AccessResourceRef.timeSeries(TIME_SERIES_ID)),
                eq(AccessPermission.READ)))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> policy.requireRead(TIME_SERIES_ID));
    }

    @Test
    void requireReadDeniesInactiveConnector() {
        when(currentActor.get()).thenReturn(client(MEASUREMENT_READ));
        when(connectorRepository.findByKeycloakClientId("local-connector-example"))
                .thenReturn(Optional.of(connector(ConnectorStatus.SUSPENDED)));

        assertThrows(AccessDeniedException.class, () -> policy.requireRead(TIME_SERIES_ID));
    }

    @Test
    void requireWriteAllowsSourceConnectorWithWriteGrant() {
        when(currentActor.get()).thenReturn(client(MEASUREMENT_WRITE));
        when(connectorRepository.findByKeycloakClientId("local-connector-example"))
                .thenReturn(Optional.of(connector(ConnectorStatus.ACTIVE)));
        when(accessAuthorizationService.isAllowed(
                eq(CONNECTOR_ID),
                eq(AccessResourceRef.timeSeries(TIME_SERIES_ID)),
                eq(AccessPermission.WRITE)))
                .thenReturn(true);

        ConnectorId result = policy.requireWrite(TIME_SERIES_ID);

        assertEquals(CONNECTOR_ID, result);
    }

    @Test
    void requireWriteBatchResolvesConnectorOnceForMultipleTimeSeries() {
        TimeSeriesId otherTimeSeriesId = new TimeSeriesId(UUID.fromString("f8403f92-b8b8-4b69-8d4f-10ad5b83b11f"));
        when(currentActor.get()).thenReturn(client(MEASUREMENT_WRITE));
        when(timeSeriesService.get(otherTimeSeriesId)).thenReturn(timeSeries(CONNECTOR_ID));
        when(connectorRepository.findByKeycloakClientId("local-connector-example"))
                .thenReturn(Optional.of(connector(ConnectorStatus.ACTIVE)));
        when(accessAuthorizationService.isAllowed(
                eq(CONNECTOR_ID),
                eq(AccessResourceRef.timeSeries(TIME_SERIES_ID)),
                eq(AccessPermission.WRITE)))
                .thenReturn(true);
        when(accessAuthorizationService.isAllowed(
                eq(CONNECTOR_ID),
                eq(AccessResourceRef.timeSeries(otherTimeSeriesId)),
                eq(AccessPermission.WRITE)))
                .thenReturn(true);

        ConnectorId result = policy.requireWriteBatch(java.util.List.of(TIME_SERIES_ID, otherTimeSeriesId));

        assertEquals(CONNECTOR_ID, result);
        verify(connectorRepository).findByKeycloakClientId("local-connector-example");
    }

    @Test
    void requireWriteDeniesActorWithoutMeasurementWriteRole() {
        when(currentActor.get()).thenReturn(client(MEASUREMENT_READ));

        assertThrows(AccessDeniedException.class, () -> policy.requireWrite(TIME_SERIES_ID));
    }

    @Test
    void requireWriteDeniesUserActorEvenWhenItHasMeasurementWrite() {
        when(currentActor.get()).thenReturn(user(MEASUREMENT_WRITE));

        assertThrows(AccessDeniedException.class, () -> policy.requireWrite(TIME_SERIES_ID));
        verify(connectorRepository, never()).findByKeycloakClientId("pegelhub-frontend");
    }

    @Test
    void requireWriteDeniesConnectorThatIsNotTimeSeriesSource() {
        when(currentActor.get()).thenReturn(client(MEASUREMENT_WRITE));
        when(timeSeriesService.get(TIME_SERIES_ID)).thenReturn(timeSeries(OTHER_CONNECTOR_ID));
        when(connectorRepository.findByKeycloakClientId("local-connector-example"))
                .thenReturn(Optional.of(connector(ConnectorStatus.ACTIVE)));

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> policy.requireWrite(TIME_SERIES_ID));

        assertThat(exception.getMessage())
                .contains(TIME_SERIES_ID.value().toString())
                .contains("not the source connector");
    }

    @Test
    void requireWriteDeniesUnownedTimeSeries() {
        when(currentActor.get()).thenReturn(client(MEASUREMENT_WRITE));
        when(timeSeriesService.get(TIME_SERIES_ID)).thenReturn(timeSeries(null));
        when(connectorRepository.findByKeycloakClientId("local-connector-example"))
                .thenReturn(Optional.of(connector(ConnectorStatus.ACTIVE)));

        assertThrows(AccessDeniedException.class, () -> policy.requireWrite(TIME_SERIES_ID));
        verify(accessAuthorizationService, never()).isAllowed(
                eq(CONNECTOR_ID),
                eq(AccessResourceRef.timeSeries(TIME_SERIES_ID)),
                eq(AccessPermission.WRITE));
    }

    @Test
    void requireWriteDeniesConnectorWithoutWriteGrant() {
        when(currentActor.get()).thenReturn(client(MEASUREMENT_WRITE));
        when(connectorRepository.findByKeycloakClientId("local-connector-example"))
                .thenReturn(Optional.of(connector(ConnectorStatus.ACTIVE)));
        when(accessAuthorizationService.isAllowed(
                eq(CONNECTOR_ID),
                eq(AccessResourceRef.timeSeries(TIME_SERIES_ID)),
                eq(AccessPermission.WRITE)))
                .thenReturn(false);

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> policy.requireWrite(TIME_SERIES_ID));

        assertThat(exception.getMessage())
                .contains(TIME_SERIES_ID.value().toString())
                .contains("missing write grant");
    }

    private static PegelHubActor user(PegelHubAuthority authority) {
        return new PegelHubActor(PegelHubActorType.USER, "user-subject", "pegelhub-frontend", Set.of(authority));
    }

    private static PegelHubActor client(PegelHubAuthority authority) {
        return new PegelHubActor(PegelHubActorType.CLIENT, null, "local-connector-example", Set.of(authority));
    }

    private static Connector connector(ConnectorStatus status) {
        return new Connector(
                CONNECTOR_ID,
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
                "local-connector-example",
                status);
    }

    private static TimeSeries timeSeries(ConnectorId sourceConnectorId) {
        return new TimeSeries(
                TIME_SERIES_ID,
                new StationId(UUID.fromString("7f65e3b7-97b4-4016-83a3-77f51332dc01")),
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
                sourceConnectorId);
    }
}
