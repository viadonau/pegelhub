package at.pegelhub.measurement.application;

import at.pegelhub.access.application.ConnectorReadAccessService;
import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.connector.domain.ConnectorType;
import at.pegelhub.connector.persistence.ConnectorRepository;
import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.station.domain.Station;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.stationowner.domain.StationOwnerId;
import at.pegelhub.security.CurrentActor;
import at.pegelhub.security.PegelHubActor;
import at.pegelhub.security.PegelHubActorType;
import at.pegelhub.timeseries.application.TimeSeriesService;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;
import at.pegelhub.timeseries.domain.SourceAssignment;
import at.pegelhub.timeseries.domain.MeasurementRepresentation;
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
import static at.pegelhub.shared.metadata.MetadataStatus.INACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MeasurementAuthorizationPolicyImplTest {

    private static final ConnectorId CONNECTOR_ID = new ConnectorId(UUID.randomUUID());
    private static final TimeSeriesId SERIES_ID = new TimeSeriesId(UUID.randomUUID());
    private static final MeasuringPointId POINT_ID = new MeasuringPointId(UUID.randomUUID());
    private static final StationId STATION_ID = new StationId(UUID.randomUUID());

    private final CurrentActor currentActor = mock(CurrentActor.class);
    private final ConnectorRepository connectors = mock(ConnectorRepository.class);
    private final TimeSeriesService timeSeries = mock(TimeSeriesService.class);
    private final ConnectorReadAccessService readAccess = mock(ConnectorReadAccessService.class);
    private final MeasurementAuthorizationPolicyImpl policy = new MeasurementAuthorizationPolicyImpl(
            currentActor, connectors, timeSeries, readAccess);

    @Test
    void resolvesActiveWriterWithoutLoadingMeasurementMetadata() {
        PegelHubActor actor = new PegelHubActor(
                PegelHubActorType.CLIENT, null, "client", Set.of(MEASUREMENT_WRITE));
        when(currentActor.get()).thenReturn(actor);
        when(connectors.findByKeycloakClientId("client")).thenReturn(Optional.of(connector(ACTIVE)));
        assertThat(policy.requireWriter()).isEqualTo(CONNECTOR_ID);
        verifyNoInteractions(timeSeries, readAccess);
    }

    @Test
    void missingGaugeZeroIsNotAnAuthorizationFailure() {
        var target = new MeasurementWriteTarget(series(MeasurementRepresentation.METRES_ABOVE_ADRIA),
                point(ACTIVE, null), station(ACTIVE));
        assertThatCode(() -> policy.requireWrite(CONNECTOR_ID, target)).doesNotThrowAnyException();
        verifyNoInteractions(currentActor, connectors, timeSeries, readAccess);
    }

    @Test
    void rejectsInactiveHierarchyAtEachLevel() {
        var series = series(MeasurementRepresentation.CANONICAL);
        assertThatThrownBy(() -> policy.requireWrite(CONNECTOR_ID, new MeasurementWriteTarget(
                series.update(INACTIVE, series.sourceAssignment()), point(ACTIVE, null), station(ACTIVE))))
                .isInstanceOf(AccessDeniedException.class).hasMessageContaining("TimeSeries");
        assertThatThrownBy(() -> policy.requireWrite(CONNECTOR_ID, new MeasurementWriteTarget(
                series, point(INACTIVE, null), station(ACTIVE))))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Measuring point");
        assertThatThrownBy(() -> policy.requireWrite(CONNECTOR_ID, new MeasurementWriteTarget(
                series, point(ACTIVE, null), station(INACTIVE))))
                .isInstanceOf(AccessDeniedException.class).hasMessageContaining("Station");
    }

    @Test
    void rejectsMissingAndDifferentSourceConnectors() {
        var series = series(MeasurementRepresentation.CANONICAL);
        assertThatThrownBy(() -> policy.requireWrite(new ConnectorId(UUID.randomUUID()),
                new MeasurementWriteTarget(series, point(ACTIVE, null), station(ACTIVE))))
                .isInstanceOf(AccessDeniedException.class).hasMessageContaining("not the source connector");
        assertThatThrownBy(() -> policy.requireWrite(CONNECTOR_ID,
                new MeasurementWriteTarget(series.update(ACTIVE, null), point(ACTIVE, null), station(ACTIVE))))
                .isInstanceOf(AccessDeniedException.class).hasMessageContaining("not the source connector");
    }

    @Test
    void rejectsUserAndClientWithoutWriteRoleBeforeResolvingConnector() {
        when(currentActor.get()).thenReturn(new PegelHubActor(PegelHubActorType.USER, "operator", null, Set.of(MEASUREMENT_WRITE)));
        assertThatThrownBy(policy::requireWriter).isInstanceOf(AccessDeniedException.class);
        when(currentActor.get()).thenReturn(new PegelHubActor(PegelHubActorType.CLIENT, null, "client", Set.of()));
        assertThatThrownBy(policy::requireWriter).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(connectors, timeSeries, readAccess);
    }

    @Test
    void targetCannotSubstituteAnUnrelatedPointOrStation() {
        var series = series(MeasurementRepresentation.CANONICAL);
        var unrelatedPoint = new MeasuringPoint(new MeasuringPointId(UUID.randomUUID()), STATION_ID, "Other", ACTIVE, null, null, null);
        assertThatThrownBy(() -> new MeasurementWriteTarget(series, unrelatedPoint, station(ACTIVE)))
                .isInstanceOf(IllegalArgumentException.class);
        var unrelatedStation = new Station(new StationId(UUID.randomUUID()), new StationOwnerId(UUID.randomUUID()), "Other", "River", ACTIVE);
        assertThatThrownBy(() -> new MeasurementWriteTarget(series, point(ACTIVE, null), unrelatedStation))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInactiveWriter() {
        when(currentActor.get()).thenReturn(new PegelHubActor(PegelHubActorType.CLIENT, null, "client", Set.of(MEASUREMENT_WRITE)));
        when(connectors.findByKeycloakClientId("client")).thenReturn(Optional.of(connector(INACTIVE)));
        assertThatThrownBy(policy::requireWriter).isInstanceOf(AccessDeniedException.class).hasMessageContaining("not active");
        verifyNoInteractions(timeSeries, readAccess);
    }

    private static Connector connector(at.pegelhub.shared.metadata.MetadataStatus status) {
        return new Connector(CONNECTOR_ID, "Connector", ConnectorType.OTHER, "client", status);
    }

    private static TimeSeries series(MeasurementRepresentation representation) {
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
