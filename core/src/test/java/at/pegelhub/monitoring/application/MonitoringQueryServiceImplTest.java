package at.pegelhub.monitoring.application;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measuringpoint.application.MeasuringPointService;
import at.pegelhub.measuringpoint.domain.BankSide;
import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.measurement.application.LatestMeasurement;
import at.pegelhub.measurement.application.MeasurementLatestQuery;
import at.pegelhub.measurement.application.MeasurementService;
import at.pegelhub.station.application.StationService;
import at.pegelhub.station.domain.Station;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.stationowner.application.StationOwnerService;
import at.pegelhub.stationowner.domain.StationOwner;
import at.pegelhub.stationowner.domain.StationOwnerId;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.timeseries.application.TimeSeriesService;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import at.pegelhub.timeseries.domain.UnitCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class MonitoringQueryServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-08-15T12:00:00Z");
    private static final TimeSeriesId SERIES_ID = new TimeSeriesId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MeasuringPointId POINT_ID = new MeasuringPointId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final StationId STATION_ID = new StationId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
    private static final StationOwnerId OWNER_ID = new StationOwnerId(UUID.fromString("44444444-4444-4444-4444-444444444444"));

    private TimeSeriesService timeSeriesService;
    private MeasuringPointService measuringPointService;
    private StationService stationService;
    private StationOwnerService ownerService;
    private MeasurementService measurementService;
    private MonitoringAuthorizationPolicy authorizationPolicy;
    private MonitoringQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        timeSeriesService = mock(TimeSeriesService.class);
        measuringPointService = mock(MeasuringPointService.class);
        stationService = mock(StationService.class);
        ownerService = mock(StationOwnerService.class);
        measurementService = mock(MeasurementService.class);
        authorizationPolicy = mock(MonitoringAuthorizationPolicy.class);
        service = new MonitoringQueryServiceImpl(
                timeSeriesService,
                measuringPointService,
                stationService,
                ownerService,
                measurementService,
                authorizationPolicy,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void assemblesCollectionAndSharesOneResolvedWindowWithMeasurementRead() {
        TimeSeries series = timeSeries();
        when(timeSeriesService.list()).thenReturn(List.of(series));
        when(measuringPointService.list()).thenReturn(List.of(measuringPoint()));
        when(stationService.list()).thenReturn(List.of(station()));
        when(measurementService.listLatestMeasurements(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(new LatestMeasurement(SERIES_ID, NOW.minusSeconds(30), 12.5)));

        MonitoringCollection result = service.readCollection(java.time.Duration.ofDays(7));

        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.timeSeries().id()).isEqualTo(SERIES_ID);
            assertThat(item.measuringPoint().id()).isEqualTo(POINT_ID);
            assertThat(item.station().id()).isEqualTo(STATION_ID);
            assertThat(item.latestMeasurement().value()).isEqualTo(12.5);
        });
        ArgumentCaptor<MeasurementLatestQuery> query = ArgumentCaptor.forClass(MeasurementLatestQuery.class);
        verify(measurementService).listLatestMeasurements(query.capture());
        assertThat(query.getValue().window().from()).isEqualTo(NOW.minus(java.time.Duration.ofDays(7)));
        assertThat(query.getValue().window().to()).isEqualTo(NOW);
    }

    @Test
    void skipsMeasurementStoreForAnEmptyCatalog() {
        when(timeSeriesService.list()).thenReturn(List.of());

        MonitoringCollection result = service.readCollection(java.time.Duration.ofDays(365));

        assertThat(result.items()).isEmpty();
        verify(measurementService, never()).listLatestMeasurements(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void assemblesDetailAndLeavesLatestMeasurementNullWhenNoValueExists() {
        when(timeSeriesService.get(SERIES_ID)).thenReturn(timeSeries());
        when(measuringPointService.get(POINT_ID)).thenReturn(measuringPoint());
        when(stationService.get(STATION_ID)).thenReturn(station());
        when(ownerService.get(OWNER_ID)).thenReturn(new StationOwner(OWNER_ID, "Owner", "viadonau", null));
        when(measurementService.listLatestMeasurements(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

        MonitoringDetail result = service.readDetail(SERIES_ID, java.time.Duration.ofDays(7));

        assertThat(result.timeSeries().id()).isEqualTo(SERIES_ID);
        assertThat(result.measuringPoint().id()).isEqualTo(POINT_ID);
        assertThat(result.station().id()).isEqualTo(STATION_ID);
        assertThat(result.stationOwner().id()).isEqualTo(OWNER_ID);
        assertThat(result.latestMeasurement()).isNull();
    }

    @Test
    void rejectsDetailWhenAReferencedRelationshipIsMissing() {
        when(timeSeriesService.get(SERIES_ID)).thenReturn(timeSeries());
        when(measuringPointService.get(POINT_ID)).thenThrow(new NotFoundException("missing"));

        assertThatThrownBy(() -> service.readDetail(SERIES_ID, java.time.Duration.ofDays(7)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing measuring point");
    }

    private static TimeSeries timeSeries() {
        return new TimeSeries(
                SERIES_ID,
                POINT_ID,
                new ObservedPropertyCode("water-level"),
                new UnitCode("cm"),
                null,
                new ConnectorId(UUID.fromString("55555555-5555-5555-5555-555555555555")));
    }

    private static MeasuringPoint measuringPoint() {
        return new MeasuringPoint(POINT_ID, STATION_ID, "Main gauge", null, null, null, BankSide.LEFT, null, null, null, null);
    }

    private static Station station() {
        return new Station(STATION_ID, OWNER_ID, "AT-001", "Station", "Danube", null);
    }
}
