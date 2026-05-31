package at.pegelhub.shared.influx;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InfluxPropertiesTest {

    @Test
    void exposesBucketSpecificDatabaseProperties() {
        InfluxProperties properties = new InfluxProperties(
                "http://localhost:8111",
                "pegelhub",
                "token",
                "data",
                "telemetry",
                "6h");

        assertThat(properties.dataDatabase())
                .isEqualTo(new DatabaseProperties("http://localhost:8111", "pegelhub", "data", "token"));
        assertThat(properties.telemetryDatabase())
                .isEqualTo(new DatabaseProperties("http://localhost:8111", "pegelhub", "telemetry", "token"));
        assertThat(properties.latestRangeDuration()).isEqualTo(new FluxDuration("6h"));
    }
}
