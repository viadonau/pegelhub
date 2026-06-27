package at.pegelhub.measurement.persistence;

import com.influxdb.client.InfluxDBClient;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measurement.application.MeasurementBucketQuery;
import at.pegelhub.measurement.application.MeasurementBucketResolution;
import at.pegelhub.measurement.application.MeasurementBucketWidth;
import at.pegelhub.measurement.application.MeasurementCursor;
import at.pegelhub.measurement.application.MeasurementListQuery;
import at.pegelhub.measurement.application.MeasurementOrder;
import at.pegelhub.measurement.application.MeasurementPageRow;
import at.pegelhub.measurement.application.MeasurementWindow;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.measurement.domain.MeasurementBucket;
import at.pegelhub.shared.influx.DatabaseProperties;
import at.pegelhub.testsupport.InfluxIntegrationTestBase;
import at.pegelhub.testsupport.PegelHubInfluxContainer;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class InfluxMeasurementRepositoryTest extends InfluxIntegrationTestBase {

    private static final DatabaseProperties PROPERTIES = new DatabaseProperties(
            "url",
            PegelHubInfluxContainer.ORG,
            PegelHubInfluxContainer.DATA_BUCKET,
            PegelHubInfluxContainer.ADMIN_TOKEN);

    private InfluxDBClient client;
    private InfluxMeasurementRepository repository;

    @BeforeEach
    void setUp() {
        client = getInfluxDBDataClient();
        repository = new InfluxMeasurementRepository(client, PROPERTIES);
    }

    @AfterEach
    void tearDown() {
        client.close();
    }

    @Test
    void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new InfluxMeasurementRepository(null, PROPERTIES));
        assertThrows(NullPointerException.class, () -> new InfluxMeasurementRepository(client, null));
    }

    @Test
    void writesReadsRangeAndLatestMeasurementData() {
        TimeSeriesId timeSeriesId = new TimeSeriesId(UUID.randomUUID());
        ConnectorId connectorId = new ConnectorId(UUID.randomUUID());
        Instant recentTimestamp = Instant.now()
                .minus(1, ChronoUnit.HOURS)
                .truncatedTo(ChronoUnit.SECONDS);
        Instant oldTimestamp = recentTimestamp.minus(5, ChronoUnit.HOURS);
        Measurement oldMeasurement = new Measurement(
                timeSeriesId,
                oldTimestamp,
                oldTimestamp.plusSeconds(1),
                10.1,
                connectorId);
        Measurement recentMeasurement = new Measurement(
                timeSeriesId,
                recentTimestamp,
                recentTimestamp.plusSeconds(1),
                11.2,
                connectorId);

        repository.storeMeasurements(List.of(oldMeasurement, recentMeasurement));

        var query = new MeasurementListQuery(
                timeSeriesId,
                new MeasurementWindow(recentTimestamp.minus(3, ChronoUnit.HOURS), recentTimestamp.plusSeconds(30), null),
                MeasurementOrder.ASC,
                100,
                null);

        assertThat(repository.findMeasurements(query))
                .containsExactly(new MeasurementPageRow(
                        recentMeasurement.observedAt(),
                        recentMeasurement.value(),
                        recentMeasurement.submittedByConnectorId()));
    }

    @Test
    void averagesMeasurementValuesAcrossConnectorTags() {
        TimeSeriesId timeSeriesId = new TimeSeriesId(UUID.randomUUID());
        ConnectorId connectorA = new ConnectorId(UUID.randomUUID());
        ConnectorId connectorB = new ConnectorId(UUID.randomUUID());
        Instant baseTimestamp = Instant.now()
                .minus(1, ChronoUnit.HOURS)
                .truncatedTo(ChronoUnit.SECONDS);
        Measurement first = new Measurement(
                timeSeriesId,
                baseTimestamp,
                baseTimestamp.plusSeconds(1),
                10.0,
                connectorA);
        Measurement second = new Measurement(
                timeSeriesId,
                baseTimestamp.plusSeconds(60),
                baseTimestamp.plusSeconds(61),
                20.0,
                connectorB);

        repository.storeMeasurements(List.of(first, second));

        var buckets = repository.findMeasurementBuckets(new MeasurementBucketQuery(
                timeSeriesId,
                new MeasurementWindow(baseTimestamp.minus(10, ChronoUnit.MINUTES), baseTimestamp.plus(10, ChronoUnit.MINUTES), null),
                MeasurementBucketResolution.explicit(new MeasurementBucketWidth(Duration.ofHours(1)))));

        assertThat(buckets)
                .singleElement()
                .satisfies(bucket -> {
                    assertThat(bucket.timeSeriesId()).isEqualTo(timeSeriesId);
                    assertThat(bucket.value()).isEqualTo(15.0);
                    assertThat(bucket.sampleCount()).isEqualTo(2);
                    assertThat(bucket.to()).isAfter(bucket.from());
                });
    }

    @Test
    void returnsSeparateMeasurementBucketsForAggregateWindows() {
        TimeSeriesId timeSeriesId = new TimeSeriesId(UUID.randomUUID());
        ConnectorId connectorId = new ConnectorId(UUID.randomUUID());
        Instant baseTimestamp = Instant.now()
                .minus(2, ChronoUnit.HOURS)
                .truncatedTo(ChronoUnit.HOURS);
        Measurement first = new Measurement(
                timeSeriesId,
                baseTimestamp,
                baseTimestamp.plusSeconds(1),
                10.0,
                connectorId);
        Measurement second = new Measurement(
                timeSeriesId,
                baseTimestamp.plus(5, ChronoUnit.MINUTES),
                baseTimestamp.plus(5, ChronoUnit.MINUTES).plusSeconds(1),
                20.0,
                connectorId);
        Measurement third = new Measurement(
                timeSeriesId,
                baseTimestamp.plus(20, ChronoUnit.MINUTES),
                baseTimestamp.plus(20, ChronoUnit.MINUTES).plusSeconds(1),
                30.0,
                connectorId);

        repository.storeMeasurements(List.of(first, second, third));

        var buckets = repository.findMeasurementBuckets(new MeasurementBucketQuery(
                timeSeriesId,
                new MeasurementWindow(baseTimestamp.minus(1, ChronoUnit.MINUTES), baseTimestamp.plus(40, ChronoUnit.MINUTES), null),
                MeasurementBucketResolution.explicit(new MeasurementBucketWidth(Duration.ofMinutes(15)))));

        assertThat(buckets)
                .hasSize(2)
                .extracting(MeasurementBucket::value, MeasurementBucket::sampleCount)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(15.0, 2L),
                        org.assertj.core.groups.Tuple.tuple(30.0, 1L));
        assertThat(buckets)
                .allSatisfy(bucket -> assertThat(bucket.to()).isEqualTo(bucket.from().plus(15, ChronoUnit.MINUTES)));
    }

    @Test
    void returnsInfluxSystemTime() {
        Instant before = Instant.now().minus(5, ChronoUnit.SECONDS);

        Instant systemTime = repository.getSystemTime();

        assertThat(systemTime).isBetween(before, Instant.now().plus(5, ChronoUnit.SECONDS));
    }

    @Test
    void missingWindowMeasurementsReturnEmptyList() {
        TimeSeriesId timeSeriesId = new TimeSeriesId(UUID.fromString("e27efad9-b947-48b1-928e-c25663597f1c"));
        var query = new MeasurementListQuery(
                timeSeriesId,
                new MeasurementWindow(Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T01:00:00Z"), null),
                MeasurementOrder.ASC,
                100,
                null);

        assertThat(repository.findMeasurements(query)).isEmpty();
    }

    @Test
    void appliesCompositeCursorOrderingInInflux() {
        TimeSeriesId timeSeriesId = new TimeSeriesId(UUID.randomUUID());
        ConnectorId connectorA = new ConnectorId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        ConnectorId connectorB = new ConnectorId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        Instant sharedTimestamp = Instant.now()
                .minus(1, ChronoUnit.HOURS)
                .truncatedTo(ChronoUnit.SECONDS);
        Measurement first = new Measurement(
                timeSeriesId,
                sharedTimestamp,
                sharedTimestamp.plusSeconds(1),
                10.0,
                connectorA);
        Measurement second = new Measurement(
                timeSeriesId,
                sharedTimestamp,
                sharedTimestamp.plusSeconds(2),
                11.0,
                connectorB);
        Measurement third = new Measurement(
                timeSeriesId,
                sharedTimestamp.plusSeconds(60),
                sharedTimestamp.plusSeconds(61),
                12.0,
                connectorA);

        repository.storeMeasurements(List.of(first, second, third));

        var query = new MeasurementListQuery(
                timeSeriesId,
                new MeasurementWindow(sharedTimestamp.minus(10, ChronoUnit.MINUTES), sharedTimestamp.plus(10, ChronoUnit.MINUTES), null),
                MeasurementOrder.ASC,
                2,
                new MeasurementCursor(sharedTimestamp, connectorA));

        assertThat(repository.findMeasurements(query))
                .extracting(MeasurementPageRow::value)
                .containsExactly(11.0, 12.0);
    }
}
