package at.pegelhub.measurement.persistence;

import at.pegelhub.measurement.application.MeasurementBucketQuery;
import at.pegelhub.measurement.application.MeasurementBucketResolution;
import at.pegelhub.measurement.application.MeasurementBucketWidth;
import at.pegelhub.measurement.application.MeasurementListQuery;
import at.pegelhub.measurement.application.MeasurementLatestQuery;
import at.pegelhub.measurement.application.MeasurementOrder;
import at.pegelhub.measurement.application.MeasurementWindow;
import at.pegelhub.shared.influx.DatabaseProperties;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

final class MeasurementFluxQueryBuilderTest {

    private static final DatabaseProperties DATABASE = new DatabaseProperties(
            "http://localhost:8111",
            "org",
            "data\"bucket",
            "token");
    private static final TimeSeriesId TIME_SERIES_ID =
            new TimeSeriesId(UUID.fromString("e27efad9-b947-48b1-928e-c25663597f1c"));

    private final MeasurementFluxQueryBuilder queryBuilder = new MeasurementFluxQueryBuilder(DATABASE);

    @Test
    void buildsBoundedMeasurementReadQuery() {
        MeasurementListQuery readQuery = new MeasurementListQuery(
                TIME_SERIES_ID,
                new MeasurementWindow(
                        Instant.parse("2026-06-17T00:00:00Z"),
                        Instant.parse("2026-06-18T00:00:00Z"),
                        null),
                MeasurementOrder.ASC,
                500);

        String query = queryBuilder.rawMeasurements(readQuery, 501);

        assertThat(query)
                .contains("from(bucket: \"data\\\"bucket\")")
                .contains("|> filter(fn: (r) => r._measurement == \"e27efad9-b947-48b1-928e-c25663597f1c\")")
                .contains("|> sort(columns: [\"_time\", \"submittedByConnectorId\"], desc: false)")
                .contains("|> limit(n: 501)")
                .doesNotContain("receivedAtRows")
                .doesNotContain("_field == \"receivedAt\"")
                .doesNotContain("join(");
        assertThat(query)
                .containsSubsequence(
                        "|> filter(fn: (r) => r._field == \"value\")",
                        "|> group(columns: [])");
    }

    @Test
    void buildsMeasurementBucketQueries() {
        MeasurementBucketQuery bucketQuery = new MeasurementBucketQuery(
                TIME_SERIES_ID,
                new MeasurementWindow(
                        Instant.parse("2026-06-17T00:00:00Z"),
                        Instant.parse("2026-06-18T00:00:00Z"),
                        null),
                MeasurementBucketResolution.explicit(new MeasurementBucketWidth(Duration.ofMinutes(15))));

        assertThat(queryBuilder.meanBuckets(bucketQuery))
                .contains("aggregateWindow(every: 15m, fn: mean, createEmpty: false, timeSrc: \"_start\")");
        assertThat(queryBuilder.countBuckets(bucketQuery))
                .contains("aggregateWindow(every: 15m, fn: count, createEmpty: false, timeSrc: \"_start\")");
    }

    @Test
    void buildsSystemTimeQuery() {
        assertThat(queryBuilder.systemTime())
                .isEqualTo("import \"system\"\n"
                        + "import \"array\"\n"
                + "array.from(rows: [{time: system.time()}])");
    }

    @Test
    void buildsOneGroupedLatestQueryForAllRequestedSeries() {
        MeasurementLatestQuery query = new MeasurementLatestQuery(
                java.util.List.of(
                        TIME_SERIES_ID,
                        new TimeSeriesId(UUID.fromString("2e27efad-b947-48b1-928e-c25663597f1c"))),
                new MeasurementWindow(
                        Instant.parse("2026-06-17T00:00:00Z"),
                        Instant.parse("2026-06-18T00:00:00Z"),
                        "1d"));

        assertThat(queryBuilder.latestMeasurements(query))
                .contains("contains(value: r._measurement, set: [\"e27efad9-b947-48b1-928e-c25663597f1c\", \"2e27efad-b947-48b1-928e-c25663597f1c\"])")
                .contains("|> group(columns: [\"_measurement\"])")
                .contains("|> sort(columns: [\"_time\", \"submittedByConnectorId\"], desc: true)")
                .contains("|> limit(n: 1)")
                .contains("|> keep(columns: [\"_measurement\", \"_time\", \"submittedByConnectorId\", \"value\"])")
                .containsSubsequence("contains(", "|> group(", "|> sort(", "|> limit(");
    }
}
