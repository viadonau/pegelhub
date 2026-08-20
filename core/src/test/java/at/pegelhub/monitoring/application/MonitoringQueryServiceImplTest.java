package at.pegelhub.monitoring.application;

import at.pegelhub.measuringpoint.application.MeasuringPointService;
import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.measurement.application.MeasurementService;
import at.pegelhub.shared.metadata.MetadataStatus;
import at.pegelhub.station.application.StationService;
import at.pegelhub.station.domain.Station;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.stationowner.application.StationOwnerService;
import at.pegelhub.stationowner.domain.StationOwner;
import at.pegelhub.stationowner.domain.StationOwnerId;
import at.pegelhub.timeseries.application.TimeSeriesService;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static at.pegelhub.shared.metadata.MetadataStatus.ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MonitoringQueryServiceImplTest {

    private static final MeasuringPointId POINT_ID = new MeasuringPointId(UUID.randomUUID());
    private static final StationId STATION_ID = new StationId(UUID.randomUUID());
    private static final StationOwnerId OWNER_ID = new StationOwnerId(UUID.randomUUID());
    private static final TimeSeriesId ACTIVE_SERIES_ID = new TimeSeriesId(UUID.randomUUID());
    private static final TimeSeriesId INACTIVE_SERIES_ID = new TimeSeriesId(UUID.randomUUID());
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-17T12:00:00Z"), ZoneOffset.UTC);

    private final TimeSeriesService timeSeries = mock(TimeSeriesService.class);
    private final MeasuringPointService points = mock(MeasuringPointService.class);
    private final StationService stations = mock(StationService.class);
    private final StationOwnerService owners = mock(StationOwnerService.class);
    private final MeasurementService measurements = mock(MeasurementService.class);
    private final MonitoringAuthorizationPolicy authorization = mock(MonitoringAuthorizationPolicy.class);
    private final MonitoringQueryServiceImpl service = new MonitoringQueryServiceImpl(
            timeSeries, points, stations, owners, measurements, authorization, CLOCK);

    @Test
    void collectionIncludesOnlyEffectivelyActiveSeriesAndLoadsLatestOnce() {
        when(timeSeries.list()).thenReturn(List.of(activeSeries(), inactiveSeries()));
        when(points.list()).thenReturn(List.of(point(ACTIVE)));
        when(stations.list()).thenReturn(List.of(station(ACTIVE)));
        when(measurements.listLatestMeasurements(any())).thenReturn(List.of());

        MonitoringCollection collection = service.readCollection(Duration.ofDays(1));

        assertThat(collection.items()).extracting(item -> item.timeSeries().id())
                .containsExactly(ACTIVE_SERIES_ID);
        verify(measurements).listLatestMeasurements(any());
    }

    @Test
    void emptyCollectionDoesNotTouchInflux() {
        when(timeSeries.list()).thenReturn(List.of());

        MonitoringCollection collection = service.readCollection(Duration.ofDays(1));

        assertThat(collection.items()).isEmpty();
        verify(measurements, never()).listLatestMeasurements(any());
    }

    @Test
    void detailReportsInactiveEffectiveStatus() {
        when(timeSeries.get(INACTIVE_SERIES_ID)).thenReturn(inactiveSeries());
        when(points.get(POINT_ID)).thenReturn(point(ACTIVE));
        when(stations.get(STATION_ID)).thenReturn(station(ACTIVE));
        when(owners.get(OWNER_ID)).thenReturn(owner());
        when(measurements.listLatestMeasurements(any())).thenReturn(List.of());

        MonitoringDetail detail = service.readDetail(INACTIVE_SERIES_ID, Duration.ofDays(1));

        assertThat(detail.status()).isEqualTo(MetadataStatus.INACTIVE);
    }

    private static TimeSeries activeSeries() {
        return new TimeSeries(ACTIVE_SERIES_ID, POINT_ID, new ObservedPropertyCode("water-level"), ACTIVE, null);
    }

    private static TimeSeries inactiveSeries() {
        return new TimeSeries(INACTIVE_SERIES_ID, POINT_ID, new ObservedPropertyCode("discharge"), MetadataStatus.INACTIVE, null);
    }

    private static MeasuringPoint point(MetadataStatus status) {
        return new MeasuringPoint(POINT_ID, STATION_ID, "Point", status, null, null, null);
    }

    private static Station station(MetadataStatus status) {
        return new Station(STATION_ID, OWNER_ID, "Station", "Danube", status);
    }

    private static StationOwner owner() {
        return new StationOwner(OWNER_ID, "Owner", "Owner", null);
    }
}
