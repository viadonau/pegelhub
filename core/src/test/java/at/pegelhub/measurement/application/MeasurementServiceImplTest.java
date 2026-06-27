package at.pegelhub.measurement.application;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.measurement.domain.MeasurementBucket;
import at.pegelhub.measurement.domain.WriteMeasurement;
import at.pegelhub.measurement.domain.WriteMeasurements;
import at.pegelhub.measurement.persistence.MeasurementRepository;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class MeasurementServiceImplTest {

    private MeasurementServiceImpl measurementService;

    private static final UUID CONNECTOR_UUID = UUID.fromString("0d9a3c87-b41a-4663-af0a-f6ec5e6a91cf");
    private static final TimeSeriesId TIME_SERIES_ID = new TimeSeriesId(UUID.fromString("8ce8c5b6-f093-4d46-b770-7239cdfa3d76"));
    private static final Instant OBSERVED_AT = Instant.parse("2026-04-25T10:15:30Z");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-17T13:00:00Z"), ZoneOffset.UTC);
    private static final ConnectorId CONNECTOR_ID = new ConnectorId(CONNECTOR_UUID);
    private static final MeasurementPageRow PAGE_ROW = new MeasurementPageRow(OBSERVED_AT, 10.5, CONNECTOR_ID);
    private static final MeasurementWindow WINDOW = new MeasurementWindow(
            Instant.parse("2026-06-16T13:00:00Z"),
            Instant.parse("2026-06-17T13:00:00Z"),
            "24h");
    private static final MeasurementRepository MEASUREMENT_REPOSITORY = mock(MeasurementRepository.class);
    private static final MeasurementAuthorizationPolicy AUTHORIZATION_POLICY = mock(MeasurementAuthorizationPolicy.class);

    @BeforeEach
    void prepare() {
        measurementService = new MeasurementServiceImpl(
                MEASUREMENT_REPOSITORY,
                AUTHORIZATION_POLICY,
                CLOCK);
        reset(MEASUREMENT_REPOSITORY, AUTHORIZATION_POLICY);
    }

    @Test
    void constructorWithNullArgsThrowsNpe() {
        assertThrows(NullPointerException.class, () -> new MeasurementServiceImpl(null, AUTHORIZATION_POLICY, CLOCK));
        assertThrows(NullPointerException.class, () -> new MeasurementServiceImpl(MEASUREMENT_REPOSITORY, null, CLOCK));
        assertThrows(NullPointerException.class, () -> new MeasurementServiceImpl(MEASUREMENT_REPOSITORY, AUTHORIZATION_POLICY, null));
    }

    @Test
    void writeMeasurementsStoresAuthorizedTimeSeriesMeasurements() {
        when(AUTHORIZATION_POLICY.requireWriteBatch(List.of(TIME_SERIES_ID))).thenReturn(new ConnectorId(CONNECTOR_UUID));

        measurementService.writeMeasurements(new WriteMeasurements(List.of(new WriteMeasurement(
                TIME_SERIES_ID,
                OBSERVED_AT,
                10.5))));

        verify(AUTHORIZATION_POLICY).requireWriteBatch(List.of(TIME_SERIES_ID));
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
    void listMeasurementsReturnsAuthorizedWindow() {
        var query = new MeasurementListQuery(TIME_SERIES_ID, WINDOW, MeasurementOrder.ASC, 100, null);
        when(MEASUREMENT_REPOSITORY.findMeasurements(query)).thenReturn(List.of(PAGE_ROW));

        MeasurementList result = measurementService.listMeasurements(query);

        assertEquals(1, result.measurements().size());
        assertEquals(PAGE_ROW, result.measurements().getFirst());
        assertEquals(false, result.truncated());
        verify(AUTHORIZATION_POLICY).requireRead(TIME_SERIES_ID);
    }

    @Test
    void listMeasurementsLimitsAndReturnsCompositeCursor() {
        MeasurementPageRow first = measurementAt("2026-06-17T11:00:00Z");
        MeasurementPageRow second = measurementAt("2026-06-17T12:00:00Z");
        var query = new MeasurementListQuery(TIME_SERIES_ID, WINDOW, MeasurementOrder.DESC, 1, null);
        when(MEASUREMENT_REPOSITORY.findMeasurements(query)).thenReturn(List.of(second, first));

        MeasurementList result = measurementService.listMeasurements(query);

        assertEquals(List.of(second), result.measurements());
        assertEquals(true, result.truncated());
        assertEquals(new MeasurementCursor(second.observedAt(), second.submittedByConnectorId()), result.nextCursor());
    }

    @Test
    void listMeasurementBucketsReturnsAuthorizedBuckets() {
        var query = new MeasurementBucketQuery(
                TIME_SERIES_ID,
                WINDOW,
                MeasurementBucketResolution.explicit(new MeasurementBucketWidth(Duration.ofMinutes(5))));
        var bucket = new MeasurementBucket(
                TIME_SERIES_ID,
                WINDOW.from(),
                WINDOW.from().plusSeconds(300),
                1.0,
                2);
        when(MEASUREMENT_REPOSITORY.findMeasurementBuckets(query)).thenReturn(List.of(bucket));

        MeasurementBucketList result = measurementService.listMeasurementBuckets(query);

        assertEquals(List.of(bucket), result.buckets());
        verify(AUTHORIZATION_POLICY).requireRead(TIME_SERIES_ID);
    }

    @Test
    void getSystemTimeDelegatesToRepository() {
        Instant ts = Instant.parse("2026-01-02T03:04:05Z");
        when(MEASUREMENT_REPOSITORY.getSystemTime()).thenReturn(ts);

        Instant result = measurementService.getSystemTime();

        assertEquals(ts, result);
    }

    private static MeasurementPageRow measurementAt(String observedAt) {
        Instant timestamp = Instant.parse(observedAt);
        return new MeasurementPageRow(
                timestamp,
                10.5,
                CONNECTOR_ID);
    }
}
