package at.pegelhub.telemetry.persistence;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.exceptions.InfluxException;
import at.pegelhub.shared.influx.DatabaseProperties;
import at.pegelhub.telemetry.domain.Telemetry;
import at.pegelhub.testsupport.InfluxIntegrationTestBase;
import at.pegelhub.testsupport.PegelHubInfluxContainer;
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

    private InfluxDBClient client;
    private InfluxTelemetryRepository repository;

    @BeforeEach
    void setUp() {
        client = getInfluxDBTelemetryClient();
        repository = new InfluxTelemetryRepository(client, PROPERTIES);
    }

    @AfterEach
    void tearDown() {
        client.close();
    }

    @Test
    void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new InfluxTelemetryRepository(null, PROPERTIES));
        assertThrows(NullPointerException.class, () -> new InfluxTelemetryRepository(client, null));
    }

    @Test
    void saveTelemetryWithNullThrowsNPE() {
        assertThrows(NullPointerException.class, () -> repository.saveTelemetry(null));
    }

    @Test
    void writesReadsRangeAndLatestTelemetryData() {
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String recentTimestamp = now.minus(1, ChronoUnit.HOURS).toString();
        String oldTimestamp = now.minus(6, ChronoUnit.HOURS).toString();
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
    void invalidRangeThrowsInfluxException() {
        assertThrows(InfluxException.class, () -> repository.getByRange(null));
        assertThrows(InfluxException.class, () -> repository.getByRange(""));
        assertThrows(InfluxException.class, () -> repository.getByRange("null"));
        assertThrows(InfluxException.class, () -> repository.getByRange("-3d"));
    }

    @Test
    void missingLatestTelemetryThrowsInfluxException() {
        UUID id = UUID.fromString("e27efad9-b947-48b1-928e-c25663597f1c");

        assertThrows(InfluxException.class, () -> repository.getLastData(id));
    }

    private static Telemetry telemetry(String id, String timestamp, int cycleTime, double temperatureWater) {
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
