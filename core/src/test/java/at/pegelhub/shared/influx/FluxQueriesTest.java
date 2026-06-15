package at.pegelhub.shared.influx;

import org.junit.jupiter.api.Test;

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
    }
}
