package at.pegelhub.measurement.application;

import at.pegelhub.access.application.ConnectorReadAccessService;
import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.connector.domain.ConnectorType;
import at.pegelhub.connector.persistence.ConnectorRepository;
import at.pegelhub.measuringpoint.application.MeasuringPointService;
import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.station.application.StationService;
import at.pegelhub.station.domain.Station;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.stationowner.domain.StationOwnerId;
import at.pegelhub.security.CurrentActor;
import at.pegelhub.security.PegelHubActor;
import at.pegelhub.security.PegelHubActorType;
import at.pegelhub.timeseries.application.TimeSeriesService;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;
import at.pegelhub.timeseries.domain.SourceAssignment;
import at.pegelhub.timeseries.domain.SourceRepresentation;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static at.pegelhub.security.PegelHubAuthority.MEASUREMENT_WRITE;
import static at.pegelhub.shared.metadata.MetadataStatus.ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MeasurementAuthorizationPolicyImplTest {

    private static final ConnectorId CONNECTOR_ID = new ConnectorId(UUID.randomUUID());
    private static final TimeSeriesId SERIES_ID = new TimeSeriesId(UUID.randomUUID());
    private static final MeasuringPointId POINT_ID = new MeasuringPointId(UUID.randomUUID());
    private static final StationId STATION_ID = new StationId(UUID.randomUUID());

    private final CurrentActor currentActor = mock(CurrentActor.class);
    private final ConnectorRepository connectors = mock(ConnectorRepository.class);
    private final TimeSeriesService timeSeries = mock(TimeSeriesService.class);
    private final MeasuringPointService points = mock(MeasuringPointService.class);
    private final StationService stations = mock(StationService.class);
    private final ConnectorReadAccessService readAccess = mock(ConnectorReadAccessService.class);
    private final MeasurementAuthorizationPolicyImpl policy = new MeasurementAuthorizationPolicyImpl(
            currentActor, connectors, timeSeries, points, stations, readAccess);

    @Test
    void capturesSourceRepresentationAndPnpAlongsideAuthorization() {
        PegelHubActor actor = new PegelHubActor(
                PegelHubActorType.CLIENT, null, "client", Set.of(MEASUREMENT_WRITE));
        when(currentActor.get()).thenReturn(actor);
        when(connectors.findByKeycloakClientId("client")).thenReturn(Optional.of(connector(ACTIVE)));
        when(timeSeries.get(SERIES_ID)).thenReturn(series(SourceRepresentation.METRES_ABOVE_ADRIA));
        when(points.get(POINT_ID)).thenReturn(point(ACTIVE, new BigDecimal("154.22")));
        when(stations.get(STATION_ID)).thenReturn(station(ACTIVE));

        MeasurementWriteAuthorization authorization = policy.requireWrite(SERIES_ID);

        assertThat(authorization.connectorId()).isEqualTo(CONNECTOR_ID);
        assertThat(authorization.forTimeSeries(SERIES_ID).representation())
                .isEqualTo(SourceRepresentation.METRES_ABOVE_ADRIA);
        assertThat(authorization.forTimeSeries(SERIES_ID).gaugeZeroElevationMAboveAdria())
                .isEqualByComparingTo("154.22");
    }

    @Test
    void rejectsInactiveHierarchy() {
        PegelHubActor actor = new PegelHubActor(
                PegelHubActorType.CLIENT, null, "client", Set.of(MEASUREMENT_WRITE));
        when(currentActor.get()).thenReturn(actor);
        when(connectors.findByKeycloakClientId("client")).thenReturn(Optional.of(connector(ACTIVE)));
        when(timeSeries.get(SERIES_ID)).thenReturn(series(SourceRepresentation.CANONICAL));
        when(points.get(POINT_ID)).thenReturn(point(at.pegelhub.shared.metadata.MetadataStatus.INACTIVE, null));

        assertThatThrownBy(() -> policy.requireWrite(SERIES_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Measuring point");
    }

    private static Connector connector(at.pegelhub.shared.metadata.MetadataStatus status) {
        return new Connector(CONNECTOR_ID, "Connector", ConnectorType.OTHER, "client", status);
    }

    private static TimeSeries series(SourceRepresentation representation) {
        return new TimeSeries(
                SERIES_ID, POINT_ID, new ObservedPropertyCode("water-level"), ACTIVE,
                new SourceAssignment(CONNECTOR_ID, representation));
    }

    private static MeasuringPoint point(at.pegelhub.shared.metadata.MetadataStatus status, BigDecimal pnp) {
        return new MeasuringPoint(POINT_ID, STATION_ID, "Point", status, null, pnp, null);
    }

    private static Station station(at.pegelhub.shared.metadata.MetadataStatus status) {
        return new Station(STATION_ID, new StationOwnerId(UUID.randomUUID()), "Station", "Danube", status);
    }
}
