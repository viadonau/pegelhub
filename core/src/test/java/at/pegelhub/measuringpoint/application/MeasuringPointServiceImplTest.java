package at.pegelhub.measuringpoint.application;

import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.measuringpoint.domain.MeasuringPointPosition;
import at.pegelhub.measuringpoint.persistence.MeasuringPointRepository;
import at.pegelhub.shared.error.MetadataConflictException;
import at.pegelhub.shared.metadata.MetadataStatus;
import at.pegelhub.station.application.StationService;
import at.pegelhub.station.domain.StationId;
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

class MeasuringPointServiceImplTest {
    private static final MeasuringPointId POINT_ID = new MeasuringPointId(
            UUID.fromString("77777777-7777-7777-7777-777777777777"));
    private static final StationId STATION_ID = new StationId(
            UUID.fromString("88888888-8888-8888-8888-888888888888"));

    private final MeasuringPointRepository points = mock(MeasuringPointRepository.class);
    private final StationService stations = mock(StationService.class);
    private final TimeSeriesRepository timeSeries = mock(TimeSeriesRepository.class);
    private final MeasuringPointServiceImpl service = new MeasuringPointServiceImpl(points, stations, timeSeries);

    @Test
    void cannotRemovePnpWhileAnAbsoluteSourceDependsOnThePoint() {
        MeasuringPoint existing = new MeasuringPoint(
                POINT_ID, STATION_ID, "Gauge", MetadataStatus.ACTIVE, MeasuringPointPosition.empty(),
                new BigDecimal("154.22"), null);
        when(points.findByIdForUpdate(POINT_ID)).thenReturn(Optional.of(existing));
        when(timeSeries.hasAbsoluteSourceFor(POINT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.update(POINT_ID,
                new UpdateMeasuringPointCommand("Gauge", MetadataStatus.ACTIVE, MeasuringPointPosition.empty(), null, null)))
                .isInstanceOf(MetadataConflictException.class);

        verify(points).findByIdForUpdate(POINT_ID);
        verify(points, never()).save(existing);
    }
}
