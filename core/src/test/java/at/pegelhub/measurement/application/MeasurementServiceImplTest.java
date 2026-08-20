package at.pegelhub.measurement.application;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.measurement.domain.WriteMeasurement;
import at.pegelhub.measurement.domain.WriteMeasurements;
import at.pegelhub.measurement.persistence.MeasurementRepository;
import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.timeseries.application.TimeSeriesService;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;
import at.pegelhub.timeseries.domain.SourceAssignment;
import at.pegelhub.timeseries.domain.SourceRepresentation;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import at.pegelhub.station.domain.StationId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static at.pegelhub.shared.metadata.MetadataStatus.ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class MeasurementServiceImplTest {

    private static final TimeSeriesId SERIES_ID = new TimeSeriesId(
            UUID.fromString("8ce8c5b6-f093-4d46-b770-7239cdfa3d76"));
    private static final MeasuringPointId POINT_ID = new MeasuringPointId(
            UUID.fromString("7f65e3b7-97b4-4016-83a3-77f51332dc01"));
    private static final ConnectorId CONNECTOR_ID = new ConnectorId(
            UUID.fromString("0d9a3c87-b41a-4663-af0a-f6ec5e6a91cf"));
    private static final Instant OBSERVED_AT = Instant.parse("2026-04-25T10:15:30Z");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-17T13:00:00Z"), ZoneOffset.UTC);

    private final MeasurementRepository repository = mock(MeasurementRepository.class);
    private final MeasurementAuthorizationPolicy authorization = mock(MeasurementAuthorizationPolicy.class);
    private final MeasurementServiceImpl service = new MeasurementServiceImpl(
            repository, authorization, CLOCK);

    @Test
    void storesCanonicalValuesUnchanged() {
        when(authorization.requireWriteBatch(List.of(SERIES_ID))).thenReturn(authorization(SourceRepresentation.CANONICAL, null));

        service.writeMeasurements(new WriteMeasurements(List.of(new WriteMeasurement(SERIES_ID, OBSERVED_AT, 12.5))));

        verify(repository).storeMeasurements(argThat(measurements ->
                measurements.size() == 1 && measurements.getFirst().value() == 12.5));
    }

    @Test
    void convertsAbsoluteWaterLevelUsingCurrentPnpWithoutRounding() {
        when(authorization.requireWriteBatch(List.of(SERIES_ID))).thenReturn(
                authorization(SourceRepresentation.METRES_ABOVE_ADRIA, new BigDecimal("154.22")));

        service.writeMeasurements(new WriteMeasurements(List.of(new WriteMeasurement(SERIES_ID, OBSERVED_AT, 157.3))));

        verify(repository).storeMeasurements(argThat(measurements ->
                measurements.size() == 1 && measurements.getFirst().value() == 308.0));
    }

    private static MeasurementWriteAuthorization authorization(
            SourceRepresentation representation, BigDecimal pnp) {
        return new MeasurementWriteAuthorization(
                CONNECTOR_ID,
                java.util.Map.of(SERIES_ID, new MeasurementWriteAuthorization.Normalization(representation, pnp)));
    }
}
