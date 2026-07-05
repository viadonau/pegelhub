package at.pegelhub.measurement.persistence;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measurement.application.MeasurementBucketQuery;
import at.pegelhub.measurement.application.MeasurementBucketResolution;
import at.pegelhub.measurement.application.MeasurementBucketWidth;
import at.pegelhub.measurement.application.MeasurementCursor;
import at.pegelhub.measurement.application.MeasurementListQuery;
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
    void buildsMeasurementPageQuery() {
        MeasurementCursor cursor = new MeasurementCursor(
                Instant.parse("2026-06-17T12:00:00Z"),
                new ConnectorId(UUID.fromString("0d9a3c87-b41a-4663-af0a-f6ec5e6a91cf")));
        MeasurementListQuery pageQuery = new MeasurementListQuery(
                TIME_SERIES_ID,
                new MeasurementWindow(
                        Instant.parse("2026-06-17T00:00:00Z"),
                        Instant.parse("2026-06-18T00:00:00Z"),
                        null),
                MeasurementOrder.ASC,
                500,
                cursor);

        String query = queryBuilder.page(pageQuery, 501);

        assertThat(query)
                .contains("from(bucket: \"data\\\"bucket\")")
                .contains("|> filter(fn: (r) => r._measurement == \"e27efad9-b947-48b1-928e-c25663597f1c\")")
                .contains(
                        "|> filter(fn: (r) => r._time > time(v: \"2026-06-17T12:00:00Z\")"
                                + " or (r._time == time(v: \"2026-06-17T12:00:00Z\")"
                                + " and r.submittedByConnectorId > \"0d9a3c87-b41a-4663-af0a-f6ec5e6a91cf\"))")
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
}
