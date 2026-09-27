package at.pegelhub.measurement.persistence;

import com.influxdb.client.InfluxDBClient;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measurement.application.MeasurementIntervalQuery;
import at.pegelhub.measurement.application.MeasurementServiceImpl;
import at.pegelhub.measurement.application.MeasurementAuthorizationPolicy;
import at.pegelhub.measurement.application.LatestMeasurement;
import at.pegelhub.measurement.application.MeasurementListQuery;
import at.pegelhub.measurement.application.MeasurementLatestQuery;
import at.pegelhub.measurement.application.MeasurementOrder;
import at.pegelhub.measurement.application.MeasurementReadRow;
import at.pegelhub.measurement.application.MeasurementWindow;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.shared.influx.DatabaseProperties;
import at.pegelhub.shared.influx.InfluxBucketOperations;
import at.pegelhub.measuringpoint.application.MeasuringPointService;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.station.application.StationService;
import at.pegelhub.timeseries.application.TimeSeriesService;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.testsupport.InfluxIntegrationTestBase;
import at.pegelhub.testsupport.PegelHubInfluxContainer;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static at.pegelhub.shared.metadata.MetadataStatus.ACTIVE;

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
                100);

        assertThat(repository.listMeasurements(query).measurements())
                .singleElement()
                .satisfies(measurement -> {
                    assertThat(measurement.observedAt()).isEqualTo(recentMeasurement.observedAt());
                    assertThat(measurement.value()).isEqualTo(recentMeasurement.value());
                    assertThat(measurement.submittedByConnectorId()).isEqualTo(recentMeasurement.submittedByConnectorId());
                });
    }

    @Test
    void returnsOneDeterministicLatestValuePerRequestedSeriesAndSkipsEmptySeries() {
        TimeSeriesId firstSeries = new TimeSeriesId(UUID.randomUUID());
        TimeSeriesId secondSeries = new TimeSeriesId(UUID.randomUUID());
        TimeSeriesId emptySeries = new TimeSeriesId(UUID.randomUUID());
        ConnectorId connectorA = new ConnectorId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        ConnectorId connectorB = new ConnectorId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        Instant sharedTimestamp = Instant.now().minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);

        repository.storeMeasurements(List.of(
                new Measurement(firstSeries, sharedTimestamp, sharedTimestamp.plusSeconds(1), 10.0, connectorA),
                new Measurement(firstSeries, sharedTimestamp, sharedTimestamp.plusSeconds(2), 11.0, connectorB),
                new Measurement(secondSeries, sharedTimestamp.plusSeconds(30), sharedTimestamp.plusSeconds(31), 20.0, connectorA)));

        var result = repository.listLatestMeasurements(new MeasurementLatestQuery(
                List.of(firstSeries, secondSeries, emptySeries),
                new MeasurementWindow(sharedTimestamp.minusSeconds(30), sharedTimestamp.plus(1, ChronoUnit.MINUTES), null)));

        assertThat(result)
                .hasSize(2)
                .extracting(latest -> latest.timeSeriesId())
                .containsExactlyInAnyOrder(firstSeries, secondSeries);
        assertThat(result)
                .filteredOn(latest -> latest.timeSeriesId().equals(firstSeries))
                .singleElement()
                .satisfies(latest -> {
                    assertThat(latest.observedAt()).isEqualTo(sharedTimestamp);
                    assertThat(latest.value()).isEqualTo(11.0);
                });
    }

    @Test
    void latestQuerySelectsCandidatesInStorageInsteadOfScanningHistoryInFlux() {
        var firstSeries = new TimeSeriesId(UUID.randomUUID());
        var secondSeries = new TimeSeriesId(UUID.randomUUID());
        var connector = new ConnectorId(UUID.randomUUID());
        Instant to = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        repository.storeMeasurements(List.of(
                new Measurement(firstSeries, to.minusSeconds(2), to, 10.0, connector),
                new Measurement(firstSeries, to.minusSeconds(1), to, 11.0, connector),
                new Measurement(secondSeries, to.minusSeconds(1), to, 20.0, connector)));
        var query = new MeasurementFluxQueryBuilder(PROPERTIES).latestMeasurements(new MeasurementLatestQuery(
                List.of(firstSeries, secondSeries), new MeasurementWindow(to.minus(365, ChronoUnit.DAYS), to, null)));

        // Check the real InfluxDB 2.2 execution plan, not a machine-dependent timing threshold.
        var profiles = client.getQueryApi().query("import \"profiler\"\n"
                + "option profiler.enabledProfilers = [\"query\"]\n" + query).stream()
                .flatMap(table -> table.getRecords().stream())
                .filter(record -> "profiler/query".equals(record.getMeasurement()))
                .toList();

        assertThat(profiles).singleElement().satisfies(profile ->
                assertThat((String) profile.getValueByKey("flux/query-plan"))
                        .contains("ReadWindowAggregate", "aggregates = [last]")
                        .doesNotContain("ReadRange", "ReadGroup"));
    }

    @Test
    void latestMeasurementsPreserveLongWindowBoundariesAndCompareConnectorCandidatesByTimeFirst() {
        var recentSeries = new TimeSeriesId(UUID.randomUUID());
        var sparseSeries = new TimeSeriesId(UUID.randomUUID());
        var outsideSeries = new TimeSeriesId(UUID.randomUUID());
        var unrequestedSeries = new TimeSeriesId(UUID.randomUUID());
        var connectorA = new ConnectorId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var connectorB = new ConnectorId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        Instant to = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant from = to.minus(365, ChronoUnit.DAYS);
        Instant recent = to.minusSeconds(30);

        // Arrival order and connector ID must not override the observation time.
        repository.storeMeasurements(List.of(
                new Measurement(recentSeries, recent, to, 20.0, connectorA),
                new Measurement(recentSeries, recent.minusSeconds(1), to, 19.0, connectorB),
                new Measurement(recentSeries, recent.minusSeconds(2), to, 18.0, connectorA),
                new Measurement(recentSeries, to, to, 999.0, connectorB),
                new Measurement(sparseSeries, from, to, 5.0, connectorA),
                new Measurement(sparseSeries, from.minusMillis(1), to, 999.0, connectorB),
                new Measurement(outsideSeries, from.minusMillis(1), to, 999.0, connectorA),
                new Measurement(unrequestedSeries, recent, to, 999.0, connectorA)));
        var window = new MeasurementWindow(from, to, null);

        assertThat(repository.listLatestMeasurements(new MeasurementLatestQuery(
                List.of(recentSeries, sparseSeries, outsideSeries), window)))
                .containsExactlyInAnyOrder(
                        new LatestMeasurement(recentSeries, recent, 20.0),
                        new LatestMeasurement(sparseSeries, from, 5.0));
        assertThat(repository.listLatestMeasurements(new MeasurementLatestQuery(List.of(sparseSeries), window)))
                .containsExactly(new LatestMeasurement(sparseSeries, from, 5.0));
        assertThat(repository.listLatestMeasurements(new MeasurementLatestQuery(List.of(outsideSeries), window)))
                .isEmpty();
    }

    @Test
    void intervalObservationReadIsSingleBoundedInputWithReplayIdentity() {
        TimeSeriesId id = new TimeSeriesId(UUID.randomUUID());
        ConnectorId writerA = new ConnectorId(UUID.randomUUID());
        ConnectorId writerB = new ConnectorId(UUID.randomUUID());
        Instant from = Instant.now().minus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS);
        Measurement first = new Measurement(id, from.plusSeconds(1), from.plusSeconds(2), 18, writerA);
        Measurement second = new Measurement(id, from.plusSeconds(1), from.plusSeconds(3), 19, writerB);
        Measurement boundary = new Measurement(id, from.plusSeconds(900), from.plusSeconds(901), 0, writerA);
        repository.storeMeasurements(List.of(first, second, boundary));
        repository.storeMeasurements(List.of(first));

        var rows = repository.listIntervalEvidence(
                id, new MeasurementWindow(from, from.plusSeconds(1_800), null), 100);

        assertThat(rows).hasSize(3);
        assertThat(rows.stream().filter(row -> row.observedAt().isBefore(boundary.observedAt())))
                .extracting(MeasurementReadRow::value).containsExactlyInAnyOrder(18.0, 19.0);
        assertThat(rows.stream().filter(row -> row.observedAt().equals(boundary.observedAt())))
                .singleElement().satisfies(row -> assertThat(row.value()).isZero());
    }

    @Test
    void intervalEvidenceAppliesOneBudgetAcrossPredecessorsAndWindowRows() {
        TimeSeriesId id = new TimeSeriesId(UUID.randomUUID());
        ConnectorId writerA = new ConnectorId(UUID.randomUUID());
        ConnectorId writerB = new ConnectorId(UUID.randomUUID());
        Instant from = Instant.now().minus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS);
        var window = new MeasurementWindow(from, from.plusSeconds(900), null);
        repository.storeMeasurements(List.of(
                new Measurement(id, from.minusSeconds(60), from.minusSeconds(59), 18, writerA),
                new Measurement(id, from.minusSeconds(30), from.minusSeconds(29), 19, writerB),
                new Measurement(id, from.plusSeconds(1), from.plusSeconds(2), 20, writerA)));

        assertThat(repository.listIntervalEvidence(id, window, 3)).hasSize(3);
        assertThatThrownBy(() -> repository.listIntervalEvidence(id, window, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("query exceeds");
        assertThat(repository.listIntervalEvidence(
                id, new MeasurementWindow(from.plusSeconds(900), from.plusSeconds(1_800), null), 2))
                .hasSize(2);
    }

    @Test
    void intervalPredecessorReadsLastRetainedObservationFromEachWriter() {
        TimeSeriesId id = new TimeSeriesId(UUID.randomUUID());
        ConnectorId writerA = new ConnectorId(UUID.randomUUID());
        ConnectorId writerB = new ConnectorId(UUID.randomUUID());
        Instant from = Instant.now().truncatedTo(ChronoUnit.HOURS);
        repository.storeMeasurements(List.of(
                new Measurement(id, from.minusSeconds(26 * 3_600), from, 17, writerA),
                new Measurement(id, from.minusSeconds(60), from, 18, writerA),
                new Measurement(id, from.minusSeconds(60), from, 18, writerB),
                new Measurement(id, from, from.plusSeconds(1), 19, writerA)));

        var predecessors = repository.listIntervalEvidence(
                        id, new MeasurementWindow(from, from.plusSeconds(900), null), 100)
                .stream()
                .filter(row -> row.observedAt().isBefore(from))
                .toList();

        assertThat(predecessors).hasSize(2);
        assertThat(predecessors).allSatisfy(row -> {
            assertThat(row.observedAt()).isEqualTo(from.minusSeconds(60));
            assertThat(row.value()).isEqualTo(18);
        });
    }

    @Test
    void lateWriteRecomputesPreviouslyClosedIntervalMean() {
        TimeSeriesId id = new TimeSeriesId(UUID.randomUUID());
        ConnectorId writer = new ConnectorId(UUID.randomUUID());
        Instant from = Instant.now().minus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS);
        var series = mock(TimeSeriesService.class);
        when(series.get(id)).thenReturn(new TimeSeries(id, new MeasuringPointId(UUID.randomUUID()),
                new ObservedPropertyCode("water-level"), ACTIVE, null));
        var service = new MeasurementServiceImpl(repository, mock(MeasurementAuthorizationPolicy.class),
                Clock.fixed(from.plusSeconds(7_200), ZoneOffset.UTC), series,
                mock(MeasuringPointService.class), mock(StationService.class));
        var query = new MeasurementIntervalQuery(id, from, from.plusSeconds(3_600), "1h", "UTC", true,
                at.pegelhub.timeseries.domain.MeasurementRepresentation.CANONICAL);
        repository.storeMeasurements(List.of(
                new Measurement(id, from.minusSeconds(1), from, 18, writer)));
        assertThat(service.listMeasurementIntervals(query).intervals().getFirst().mean()).isEqualTo(18);

        repository.storeMeasurements(List.of(
                new Measurement(id, from.plusSeconds(1_800), from.plusSeconds(1_801), 19, writer)));
        assertThat(service.listMeasurementIntervals(query).intervals().getFirst().mean()).isEqualTo(18.5);
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
                100);

        assertThat(repository.listMeasurements(query).measurements()).isEmpty();
    }

    @Test
    void ordersDuplicateObservedTimesDeterministicallyByConnectorId() {
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

        MeasurementWindow window = new MeasurementWindow(
                sharedTimestamp.minus(10, ChronoUnit.MINUTES),
                sharedTimestamp.plus(10, ChronoUnit.MINUTES),
                null);
        var ascendingQuery = new MeasurementListQuery(
                timeSeriesId,
                window,
                MeasurementOrder.ASC,
                3);
        var descendingQuery = new MeasurementListQuery(
                timeSeriesId,
                window,
                MeasurementOrder.DESC,
                3);

        assertThat(repository.listMeasurements(ascendingQuery).measurements())
                .extracting(MeasurementReadRow::value)
                .containsExactly(10.0, 11.0, 12.0);
        assertThat(repository.listMeasurements(descendingQuery).measurements())
                .extracting(MeasurementReadRow::value)
                .containsExactly(12.0, 11.0, 10.0);
    }

    @Test
    void enforcesLimitAndReportsTruncatedWindow() {
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
                1);

        var result = repository.listMeasurements(query);

        assertThat(result.truncated()).isTrue();
        assertThat(result.measurements())
                .singleElement()
                .satisfies(measurement -> assertThat(measurement.value()).isEqualTo(10.0));
    }

    @Test
    void returnsMeasurementsInDescendingOrder() {
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
                100);

        assertThat(repository.listMeasurements(query).measurements())
                .extracting(MeasurementReadRow::value)
                .containsExactly(11.0, 10.0);
    }

    @Test
    void returnsLatestMeasurementThroughDescendingLimitOneRead() {
        TimeSeriesId timeSeriesId = new TimeSeriesId(UUID.randomUUID());
        ConnectorId connectorId = new ConnectorId(UUID.randomUUID());
        Instant baseTimestamp = Instant.now()
                .minus(1, ChronoUnit.HOURS)
                .truncatedTo(ChronoUnit.SECONDS);
        repository.storeMeasurements(List.of(
                new Measurement(timeSeriesId, baseTimestamp, baseTimestamp.plusSeconds(1), 10.0, connectorId),
                new Measurement(timeSeriesId, baseTimestamp.plusSeconds(60), baseTimestamp.plusSeconds(61), 11.0, connectorId)));

        var result = repository.listMeasurements(new MeasurementListQuery(
                timeSeriesId,
                new MeasurementWindow(baseTimestamp.minusSeconds(1), baseTimestamp.plusSeconds(120), null),
                MeasurementOrder.DESC,
                1));

        assertThat(result.truncated()).isTrue();
        assertThat(result.measurements())
                .singleElement()
                .satisfies(measurement -> assertThat(measurement.value()).isEqualTo(11.0));
    }
}
