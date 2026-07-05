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
import at.pegelhub.shared.influx.InfluxBucketOperations;
import at.pegelhub.testsupport.InfluxIntegrationTestBase;
import at.pegelhub.testsupport.PegelHubInfluxContainer;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
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
        var influx = new InfluxBucketOperations(client, PROPERTIES);
        repository = new InfluxMeasurementRepository(
                influx,
                new InfluxMeasurementPointMapper(),
                new MeasurementFluxQueryBuilder(PROPERTIES),
                new MeasurementFluxRowMapper());
    }

    @AfterEach
    void tearDown() {
        client.close();
    }

    @Test
    void constructorWithNullArgsThrowsNPE() {
        var influx = new InfluxBucketOperations(client, PROPERTIES);
        var pointMapper = new InfluxMeasurementPointMapper();
        var queryBuilder = new MeasurementFluxQueryBuilder(PROPERTIES);
        var rowMapper = new MeasurementFluxRowMapper();

        assertThrows(NullPointerException.class, () ->
                new InfluxMeasurementRepository(null, pointMapper, queryBuilder, rowMapper));
        assertThrows(NullPointerException.class, () ->
                new InfluxMeasurementRepository(influx, null, queryBuilder, rowMapper));
        assertThrows(NullPointerException.class, () ->
                new InfluxMeasurementRepository(influx, pointMapper, null, rowMapper));
        assertThrows(NullPointerException.class, () ->
                new InfluxMeasurementRepository(influx, pointMapper, queryBuilder, null));
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

        assertThat(repository.listMeasurements(query).measurements())
                .singleElement()
                .satisfies(measurement -> {
                    assertThat(measurement.observedAt()).isEqualTo(recentMeasurement.observedAt());
                    assertThat(measurement.value()).isEqualTo(recentMeasurement.value());
                    assertThat(measurement.submittedByConnectorId()).isEqualTo(recentMeasurement.submittedByConnectorId());
                });
    }

    @Test
    void averagesMeasurementValuesAcrossConnectorTags() {
        TimeSeriesId timeSeriesId = new TimeSeriesId(UUID.randomUUID());
        ConnectorId connectorA = new ConnectorId(UUID.randomUUID());
        ConnectorId connectorB = new ConnectorId(UUID.randomUUID());
        Instant baseTimestamp = Instant.now()
                .minus(2, ChronoUnit.HOURS)
                .truncatedTo(ChronoUnit.HOURS)
                .plus(5, ChronoUnit.MINUTES);
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

        var result = repository.listMeasurementBuckets(new MeasurementBucketQuery(
                timeSeriesId,
                new MeasurementWindow(
                        baseTimestamp.truncatedTo(ChronoUnit.HOURS),
                        baseTimestamp.truncatedTo(ChronoUnit.HOURS).plus(1, ChronoUnit.HOURS),
                        null),
                MeasurementBucketResolution.explicit(new MeasurementBucketWidth(Duration.ofHours(1)))));

        assertThat(result.buckets())
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

        var buckets = repository.listMeasurementBuckets(new MeasurementBucketQuery(
                timeSeriesId,
                new MeasurementWindow(baseTimestamp.minus(1, ChronoUnit.MINUTES), baseTimestamp.plus(40, ChronoUnit.MINUTES), null),
                MeasurementBucketResolution.explicit(new MeasurementBucketWidth(Duration.ofMinutes(15))))).buckets();

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

        assertThat(repository.listMeasurements(query).measurements()).isEmpty();
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

        assertThat(repository.listMeasurements(query).measurements())
                .extracting(MeasurementPageRow::value)
                .containsExactly(11.0, 12.0);
    }

    @Test
    void returnsTruncatedPageAndNextCursor() {
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

        repository.storeMeasurements(List.of(first, second));

        var query = new MeasurementListQuery(
                timeSeriesId,
                new MeasurementWindow(sharedTimestamp.minus(10, ChronoUnit.MINUTES), sharedTimestamp.plus(10, ChronoUnit.MINUTES), null),
                MeasurementOrder.ASC,
                1,
                null);

        var result = repository.listMeasurements(query);

        assertThat(result.truncated()).isTrue();
        assertThat(result.measurements())
                .singleElement()
                .satisfies(measurement -> assertThat(measurement.value()).isEqualTo(10.0));
        assertThat(result.nextCursor()).isEqualTo(new MeasurementCursor(sharedTimestamp, connectorA));
    }

    @Test
    void returnsDescendingMeasurementPages() {
        TimeSeriesId timeSeriesId = new TimeSeriesId(UUID.randomUUID());
        ConnectorId connectorId = new ConnectorId(UUID.randomUUID());
        Instant baseTimestamp = Instant.now()
                .minus(1, ChronoUnit.HOURS)
                .truncatedTo(ChronoUnit.SECONDS);
        Measurement first = new Measurement(
                timeSeriesId,
                baseTimestamp,
                baseTimestamp.plusSeconds(1),
                10.0,
                connectorId);
        Measurement second = new Measurement(
                timeSeriesId,
                baseTimestamp.plusSeconds(60),
                baseTimestamp.plusSeconds(61),
                11.0,
                connectorId);

        repository.storeMeasurements(List.of(first, second));

        var query = new MeasurementListQuery(
                timeSeriesId,
                new MeasurementWindow(baseTimestamp.minus(10, ChronoUnit.MINUTES), baseTimestamp.plus(10, ChronoUnit.MINUTES), null),
                MeasurementOrder.DESC,
                100,
                null);

        assertThat(repository.listMeasurements(query).measurements())
                .extracting(MeasurementPageRow::value)
                .containsExactly(11.0, 10.0);
    }
}
