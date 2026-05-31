package at.pegelhub.telemetry.persistence;

import at.pegelhub.shared.influx.DatabaseProperties;
import at.pegelhub.shared.influx.FluxDuration;
import at.pegelhub.telemetry.domain.Telemetry;
import at.pegelhub.testsupport.InfluxIntegrationTestBase;
import at.pegelhub.testsupport.PegelHubInfluxContainer;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.exceptions.InfluxException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class InfluxTelemetryRepositoryTest extends InfluxIntegrationTestBase {

    private static final DatabaseProperties PROPERTIES = new DatabaseProperties(
            "url",
            PegelHubInfluxContainer.ORG,
            PegelHubInfluxContainer.TELEMETRY_BUCKET,
            PegelHubInfluxContainer.ADMIN_TOKEN);
    private static final FluxDuration LATEST_RANGE = new FluxDuration("72h");

    private InfluxDBClient client;
    private InfluxTelemetryRepository repository;

    @BeforeEach
    void setUp() {
        client = getInfluxDBTelemetryClient();
        repository = new InfluxTelemetryRepository(client, PROPERTIES, LATEST_RANGE);
    }

    @AfterEach
    void tearDown() {
        client.close();
    }

    @Test
    void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new InfluxTelemetryRepository(null, PROPERTIES, LATEST_RANGE));
        assertThrows(NullPointerException.class, () -> new InfluxTelemetryRepository(client, null, LATEST_RANGE));
        assertThrows(NullPointerException.class, () -> new InfluxTelemetryRepository(client, PROPERTIES, null));
    }

    @Test
    void writesReadsRangeAndLatestTelemetryData() {
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant recentTimestamp = now.minus(1, ChronoUnit.HOURS);
        Instant oldTimestamp = now.minus(6, ChronoUnit.HOURS);
        Telemetry oldTelemetry = telemetry(id, oldTimestamp, 11, 15.1);
        Telemetry recentTelemetry = telemetry(id, recentTimestamp, 12, 16.2);

        assertThat(repository.saveTelemetry(oldTelemetry)).isEqualTo(oldTelemetry);
        assertThat(repository.saveTelemetry(recentTelemetry)).isEqualTo(recentTelemetry);

        assertThat(repository.getByRange("3h"))
                .filteredOn(telemetry -> id.equals(telemetry.measurement()))
                .containsExactly(recentTelemetry);
        assertThat(repository.getLastData(UUID.fromString(id))).isEqualTo(recentTelemetry);
    }

    @Test
    void invalidRangeThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> repository.getByRange(null));
        assertThrows(IllegalArgumentException.class, () -> repository.getByRange(""));
        assertThrows(IllegalArgumentException.class, () -> repository.getByRange("null"));
        assertThrows(IllegalArgumentException.class, () -> repository.getByRange("-3d"));
    }

    @Test
    void missingLatestTelemetryThrowsInfluxException() {
        UUID id = UUID.fromString("e27efad9-b947-48b1-928e-c25663597f1c");

        assertThrows(InfluxException.class, () -> repository.getLastData(id));
    }

    private static Telemetry telemetry(String id, Instant timestamp, int cycleTime, double temperatureWater) {
        return new Telemetry(
                id,
                "10.0.0.1",
                "203.0.113.1",
                timestamp,
                cycleTime,
                temperatureWater,
                20.5,
                12.1,
                24.2,
                1.3,
                2.4,
                80.5);
    }
}
