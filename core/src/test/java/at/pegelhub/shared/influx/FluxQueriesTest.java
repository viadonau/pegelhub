package at.pegelhub.shared.influx;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measurement.application.MeasurementCursor;
import at.pegelhub.measurement.application.MeasurementOrder;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FluxQueriesTest {

    private static final DatabaseProperties DATABASE = new DatabaseProperties(
            "http://localhost:8111",
            "org",
            "data\"bucket",
            "token");

    @Test
    void buildsEscapedRangeQuery() {
        assertThat(FluxQueries.range(DATABASE, new FluxDuration("3h")))
                .isEqualTo("from(bucket: \"data\\\"bucket\") |> range(start: -3h)");
    }

    @Test
    void buildsMeasurementQueries() {
        UUID id = UUID.fromString("e27efad9-b947-48b1-928e-c25663597f1c");

        assertThat(FluxQueries.latestMeasurement(DATABASE, id, new FluxDuration("6h")))
                .isEqualTo("from(bucket: \"data\\\"bucket\") |> range(start: -6h)"
                        + " |> filter(fn: (r) => r._measurement == \"e27efad9-b947-48b1-928e-c25663597f1c\") |> last()");
        assertThat(FluxQueries.meanMeasurement(DATABASE, id, new FluxDuration("7d")))
                .endsWith(" |> filter(fn: (r) => r._measurement == \"e27efad9-b947-48b1-928e-c25663597f1c\")"
                        + " |> filter(fn: (r) => r._field == \"value\")"
                        + " |> group(columns: [\"_measurement\"]) |> mean()");
        assertThat(FluxQueries.countMeasurement(DATABASE, id, new FluxDuration("7d")))
                .endsWith(" |> filter(fn: (r) => r._measurement == \"e27efad9-b947-48b1-928e-c25663597f1c\")"
                        + " |> filter(fn: (r) => r._field == \"value\")"
                        + " |> group(columns: [\"_measurement\"]) |> count()");
        assertThat(FluxQueries.meanMeasurementBuckets(
                DATABASE,
                id,
                Instant.parse("2026-06-17T00:00:00Z"),
                Instant.parse("2026-06-18T00:00:00Z"),
                new FluxDuration("15m")))
                .contains("aggregateWindow(every: 15m, fn: mean, createEmpty: false, timeSrc: \"_start\")");
    }

    @Test
    void buildsMeasurementPageQuery() {
        UUID id = UUID.fromString("e27efad9-b947-48b1-928e-c25663597f1c");
        MeasurementCursor cursor = new MeasurementCursor(
                Instant.parse("2026-06-17T12:00:00Z"),
                new ConnectorId(UUID.fromString("0d9a3c87-b41a-4663-af0a-f6ec5e6a91cf")));

        String query = FluxQueries.measurementPage(
                DATABASE,
                id,
                Instant.parse("2026-06-17T00:00:00Z"),
                Instant.parse("2026-06-18T00:00:00Z"),
                MeasurementOrder.ASC,
                501,
                cursor);

        assertThat(query)
                .contains("from(bucket: \"data\\\"bucket\")")
                .contains("|> filter(fn: (r) => r._measurement == \"e27efad9-b947-48b1-928e-c25663597f1c\")")
                .contains("|> filter(fn: (r) => r._time > time(v: \"2026-06-17T12:00:00Z\") or (r._time == time(v: \"2026-06-17T12:00:00Z\") and r.submittedByConnectorId > \"0d9a3c87-b41a-4663-af0a-f6ec5e6a91cf\"))")
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
    void buildsSystemTimeQuery() {
        assertThat(FluxQueries.systemTime())
                .isEqualTo("import \"system\"\n"
                        + "import \"array\"\n"
                        + "array.from(rows: [{time: system.time()}])");
    }
}
