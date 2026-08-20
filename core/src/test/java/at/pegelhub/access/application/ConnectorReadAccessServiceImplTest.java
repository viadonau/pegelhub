package at.pegelhub.access.application;

import at.pegelhub.access.persistence.ConnectorStationReadAccessRepository;
import at.pegelhub.access.persistence.ConnectorTimeSeriesReadAccessRepository;
import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.connector.domain.ConnectorType;
import at.pegelhub.connector.persistence.ConnectorRepository;
import at.pegelhub.measuringpoint.application.MeasuringPointService;
import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.measuringpoint.domain.MeasuringPointPosition;
import at.pegelhub.station.application.StationService;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.timeseries.application.TimeSeriesService;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConnectorReadAccessServiceImplTest {
    private static final ConnectorId CONNECTOR_ID = new ConnectorId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final StationId STATION_ID = new StationId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final MeasuringPointId POINT_ID = new MeasuringPointId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
    private static final TimeSeriesId SERIES_ID = new TimeSeriesId(UUID.fromString("44444444-4444-4444-4444-444444444444"));

    private final ConnectorRepository connectors = mock(ConnectorRepository.class);
    private final StationService stations = mock(StationService.class);
    private final TimeSeriesService timeSeries = mock(TimeSeriesService.class);
    private final MeasuringPointService measuringPoints = mock(MeasuringPointService.class);
    private final ConnectorStationReadAccessRepository stationAccess = mock(ConnectorStationReadAccessRepository.class);
    private final ConnectorTimeSeriesReadAccessRepository timeSeriesAccess = mock(ConnectorTimeSeriesReadAccessRepository.class);
    private final ConnectorReadAccessServiceImpl service = new ConnectorReadAccessServiceImpl(
            connectors, stations, timeSeries, measuringPoints, stationAccess, timeSeriesAccess);

    @Test
    void grantsStationAccessThroughAtomicRepositoryOperation() {
        when(connectors.findById(CONNECTOR_ID)).thenReturn(Optional.of(Connector.create("Connector", ConnectorType.OTHER)));

        service.grantStation(CONNECTOR_ID, STATION_ID);

        verify(stations).get(STATION_ID);
        verify(stationAccess).insertIfAbsent(CONNECTOR_ID.value(), STATION_ID.value());
    }

    @Test
    void grantsTimeSeriesAccessAfterCheckingTheSeriesExists() {
        when(connectors.findById(CONNECTOR_ID)).thenReturn(Optional.of(Connector.create("Connector", ConnectorType.OTHER)));
        when(timeSeries.get(SERIES_ID)).thenReturn(new TimeSeries(
                SERIES_ID, POINT_ID, new ObservedPropertyCode("water-level"), null, null));

        service.grantTimeSeries(CONNECTOR_ID, SERIES_ID);

        verify(timeSeriesAccess).insertIfAbsent(CONNECTOR_ID.value(), SERIES_ID.value());
    }

    @Test
    void allowsEitherExplicitSeriesOrStationAccess() {
        var series = new TimeSeries(SERIES_ID, POINT_ID, new ObservedPropertyCode("water-level"), null, null);
        var point = new MeasuringPoint(POINT_ID, STATION_ID, "Point", null, MeasuringPointPosition.empty(), null, null);
        when(timeSeries.get(SERIES_ID)).thenReturn(series);
        when(measuringPoints.get(POINT_ID)).thenReturn(point);
        when(timeSeriesAccess.existsByConnectorIdAndTimeSeriesId(CONNECTOR_ID.value(), SERIES_ID.value()))
                .thenReturn(false);
        when(stationAccess.existsByConnectorIdAndStationId(CONNECTOR_ID.value(), STATION_ID.value()))
                .thenReturn(true);

        assertThat(service.allows(CONNECTOR_ID, SERIES_ID)).isTrue();
    }
}
