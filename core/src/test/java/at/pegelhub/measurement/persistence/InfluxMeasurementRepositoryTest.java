package at.pegelhub.measurement.persistence;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.exceptions.InfluxException;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.shared.influx.DatabaseProperties;
import at.pegelhub.testsupport.InfluxIntegrationTestBase;
import at.pegelhub.testsupport.PegelHubInfluxContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
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
        repository = new InfluxMeasurementRepository(client, PROPERTIES);
    }

    @AfterEach
    void tearDown() {
        client.close();
    }

    @Test
    void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new InfluxMeasurementRepository(null, PROPERTIES));
        assertThrows(NullPointerException.class, () -> new InfluxMeasurementRepository(client, null));
    }

    @Test
    void writesReadsRangeAndLatestMeasurementData() {
        UUID id = UUID.randomUUID();
        LocalDateTime recentTimestamp = LocalDateTime.now(ZoneOffset.UTC)
                .minusHours(1)
                .truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime oldTimestamp = recentTimestamp.minusHours(5);
        Measurement oldMeasurement = new Measurement(
                id,
                oldTimestamp,
                Map.of("waterLevel", 10.1, "flow", 20.1),
                Map.of("quality", "old"));
        Measurement recentMeasurement = new Measurement(
                id,
                recentTimestamp,
                Map.of("waterLevel", 11.2, "flow", 21.2),
                Map.of("quality", "recent"));

        repository.storeMeasurements(List.of(oldMeasurement, recentMeasurement));

        assertThat(repository.getByIDAndRange(id, "3h")).containsExactly(recentMeasurement);
        assertThat(repository.getLastData(id)).isEqualTo(recentMeasurement);
    }

    @Test
    void invalidRangeThrowsInfluxException() {
        assertThrows(InfluxException.class, () -> repository.getByRange(null));
        assertThrows(InfluxException.class, () -> repository.getByRange(""));
        assertThrows(InfluxException.class, () -> repository.getByRange("null"));
        assertThrows(InfluxException.class, () -> repository.getByRange("-3d"));
    }

    @Test
    void missingLatestMeasurementThrowsInfluxException() {
        UUID id = UUID.fromString("e27efad9-b947-48b1-928e-c25663597f1c");

        assertThrows(InfluxException.class, () -> repository.getLastData(id));
    }
}
