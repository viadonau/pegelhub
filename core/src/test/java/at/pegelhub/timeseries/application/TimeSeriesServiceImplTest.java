package at.pegelhub.timeseries.application;

import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.connector.domain.ConnectorType;
import at.pegelhub.connector.persistence.ConnectorRepository;
import at.pegelhub.measuringpoint.application.MeasuringPointService;
import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.measuringpoint.domain.MeasuringPointPosition;
import at.pegelhub.shared.error.MetadataConflictException;
import at.pegelhub.shared.metadata.MetadataStatus;
import at.pegelhub.station.application.StationService;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;
import at.pegelhub.timeseries.domain.SourceAssignment;
import at.pegelhub.timeseries.domain.SourceRepresentation;
import at.pegelhub.timeseries.persistence.TimeSeriesRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TimeSeriesServiceImplTest {
    private static final MeasuringPointId POINT_ID = new MeasuringPointId(
            UUID.fromString("99999999-9999-9999-9999-999999999999"));
    private static final ConnectorId CONNECTOR_ID = new ConnectorId(
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));

    private final TimeSeriesRepository timeSeries = mock(TimeSeriesRepository.class);
    private final MeasuringPointService measuringPoints = mock(MeasuringPointService.class);
    private final StationService stations = mock(StationService.class);
    private final ConnectorRepository connectors = mock(ConnectorRepository.class);
    private final TimeSeriesServiceImpl service = new TimeSeriesServiceImpl(timeSeries, measuringPoints, stations, connectors);

    @Test
    void absoluteWaterLevelSourceRequiresPnp() {
        MeasuringPoint point = new MeasuringPoint(
                POINT_ID, new StationId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")), "Gauge", MetadataStatus.ACTIVE,
                MeasuringPointPosition.empty(), null, null);
        when(measuringPoints.getForUpdate(POINT_ID)).thenReturn(point);
        when(measuringPoints.get(POINT_ID)).thenReturn(point);

        assertThatThrownBy(() -> service.create(new CreateTimeSeriesCommand(
                POINT_ID,
                new ObservedPropertyCode("water-level"),
                MetadataStatus.ACTIVE,
                new SourceAssignment(CONNECTOR_ID, SourceRepresentation.METRES_ABOVE_ADRIA))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gauge zero elevation");
        verify(timeSeries, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void inactiveSourceConnectorIsRejected() {
        MeasuringPoint point = new MeasuringPoint(
                POINT_ID, new StationId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")), "Gauge", MetadataStatus.ACTIVE,
                MeasuringPointPosition.empty(), new BigDecimal("154.22"), null);
        when(measuringPoints.getForUpdate(POINT_ID)).thenReturn(point);
        when(measuringPoints.get(POINT_ID)).thenReturn(point);
        Connector inactive = Connector.create("Connector", ConnectorType.OTHER)
                .bind("client", MetadataStatus.INACTIVE);
        when(connectors.findById(CONNECTOR_ID)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.create(new CreateTimeSeriesCommand(
                POINT_ID,
                new ObservedPropertyCode("water-level"),
                MetadataStatus.ACTIVE,
                new SourceAssignment(CONNECTOR_ID, SourceRepresentation.METRES_ABOVE_ADRIA))))
                .isInstanceOf(MetadataConflictException.class);
    }
}
