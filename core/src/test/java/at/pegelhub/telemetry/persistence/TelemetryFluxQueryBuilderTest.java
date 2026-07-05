package at.pegelhub.telemetry.persistence;

import at.pegelhub.shared.duration.PegelhubDurationLiteral;
import at.pegelhub.shared.influx.DatabaseProperties;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

final class TelemetryFluxQueryBuilderTest {

    private static final DatabaseProperties DATABASE = new DatabaseProperties(
            "http://localhost:8111",
            "org",
            "telemetry\"bucket",
            "token");

    private final TelemetryFluxQueryBuilder queryBuilder = new TelemetryFluxQueryBuilder(DATABASE);

    @Test
    void buildsEscapedRangeQuery() {
        assertThat(queryBuilder.range(new PegelhubDurationLiteral("3h")))
                .isEqualTo("from(bucket: \"telemetry\\\"bucket\") |> range(start: -3h)");
    }

    @Test
    void buildsLatestTelemetryQuery() {
        UUID id = UUID.fromString("e27efad9-b947-48b1-928e-c25663597f1c");

        assertThat(queryBuilder.latestTelemetry(id, new PegelhubDurationLiteral("6h")))
                .isEqualTo("from(bucket: \"telemetry\\\"bucket\") |> range(start: -6h)"
                        + " |> filter(fn: (r) => r._measurement == \"e27efad9-b947-48b1-928e-c25663597f1c\") |> last()");
    }
}
