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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private static final MeasurementReadRow READ_ROW = new MeasurementReadRow(OBSERVED_AT, 10.5, CONNECTOR_ID);
    private static final MeasurementWindow WINDOW = new MeasurementWindow(
            Instant.parse("2026-06-16T13:00:00Z"),
            Instant.parse("2026-06-17T13:00:00Z"),
            "24h");
    private static final MeasurementAuthorizationPolicy AUTHORIZATION_POLICY = mock(MeasurementAuthorizationPolicy.class);
    private FakeMeasurementRepository measurementRepository;

    @BeforeEach
    void prepare() {
        measurementRepository = new FakeMeasurementRepository();
        measurementService = new MeasurementServiceImpl(
                measurementRepository,
                AUTHORIZATION_POLICY,
                CLOCK);
        reset(AUTHORIZATION_POLICY);
    }

    @Test
    void constructorWithNullArgsThrowsNpe() {
        assertThrows(NullPointerException.class, () -> new MeasurementServiceImpl(null, AUTHORIZATION_POLICY, CLOCK));
        assertThrows(NullPointerException.class, () -> new MeasurementServiceImpl(measurementRepository, null, CLOCK));
        assertThrows(NullPointerException.class, () -> new MeasurementServiceImpl(measurementRepository, AUTHORIZATION_POLICY, null));
    }

    @Test
    void writeMeasurementsStoresAuthorizedTimeSeriesMeasurements() {
        when(AUTHORIZATION_POLICY.requireWriteBatch(List.of(TIME_SERIES_ID))).thenReturn(new ConnectorId(CONNECTOR_UUID));

        measurementService.writeMeasurements(new WriteMeasurements(List.of(new WriteMeasurement(
                TIME_SERIES_ID,
                OBSERVED_AT,
                10.5))));

        verify(AUTHORIZATION_POLICY).requireWriteBatch(List.of(TIME_SERIES_ID));
        assertEquals(1, measurementRepository.storedMeasurements.size());
        Measurement stored = measurementRepository.storedMeasurements.getFirst();
        assertEquals(TIME_SERIES_ID, stored.timeSeriesId());
        assertEquals(OBSERVED_AT, stored.observedAt());
        assertEquals(CLOCK.instant(), stored.receivedAt());
        assertEquals(10.5, stored.value());
        assertEquals(new ConnectorId(CONNECTOR_UUID), stored.submittedByConnectorId());
    }

    @Test
    void listMeasurementsReturnsAuthorizedWindow() {
        var query = new MeasurementListQuery(TIME_SERIES_ID, WINDOW, MeasurementOrder.ASC, 100);
        MeasurementList repositoryResult = new MeasurementList(query, false, List.of(READ_ROW));
        measurementRepository.measurementList = repositoryResult;

        MeasurementList result = measurementService.listMeasurements(query);

        assertSame(repositoryResult, result);
        assertEquals(query, measurementRepository.listQuery);
        verify(AUTHORIZATION_POLICY).requireRead(TIME_SERIES_ID);
    }

    @Test
    void listMeasurementsDelegatesBoundedRepositoryResult() {
        MeasurementReadRow second = measurementAt("2026-06-17T12:00:00Z");
        var query = new MeasurementListQuery(TIME_SERIES_ID, WINDOW, MeasurementOrder.DESC, 1);
        measurementRepository.measurementList = new MeasurementList(query, true, List.of(second));

        MeasurementList result = measurementService.listMeasurements(query);

        assertEquals(List.of(second), result.measurements());
        assertEquals(true, result.truncated());
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
        MeasurementBucketList repositoryResult = new MeasurementBucketList(query, List.of(bucket));
        measurementRepository.bucketList = repositoryResult;

        MeasurementBucketList result = measurementService.listMeasurementBuckets(query);

        assertSame(repositoryResult, result);
        assertEquals(query, measurementRepository.bucketQuery);
        verify(AUTHORIZATION_POLICY).requireRead(TIME_SERIES_ID);
    }

    @Test
    void getSystemTimeDelegatesToRepository() {
        Instant ts = Instant.parse("2026-01-02T03:04:05Z");
        measurementRepository.systemTime = ts;

        Instant result = measurementService.getSystemTime();

        assertEquals(ts, result);
    }

    private static MeasurementReadRow measurementAt(String observedAt) {
        Instant timestamp = Instant.parse(observedAt);
        return new MeasurementReadRow(
                timestamp,
                10.5,
                CONNECTOR_ID);
    }

    private static final class FakeMeasurementRepository implements MeasurementRepository {

        private final List<Measurement> storedMeasurements = new ArrayList<>();
        private MeasurementListQuery listQuery;
        private MeasurementBucketQuery bucketQuery;
        private MeasurementList measurementList;
        private MeasurementBucketList bucketList;
        private Instant systemTime;

        @Override
        public void storeMeasurements(List<Measurement> measurements) {
            storedMeasurements.addAll(measurements);
        }

        @Override
        public MeasurementList listMeasurements(MeasurementListQuery query) {
            listQuery = query;
            return measurementList;
        }

        @Override
        public MeasurementBucketList listMeasurementBuckets(MeasurementBucketQuery query) {
            bucketQuery = query;
            return bucketList;
        }

        @Override
        public List<LatestMeasurement> listLatestMeasurements(MeasurementLatestQuery query) {
            return List.of();
        }

        @Override
        public Instant getSystemTime() {
            return systemTime;
        }
    }
}
