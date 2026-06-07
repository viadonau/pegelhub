package at.pegelhub.measurement.persistence;

import com.influxdb.client.InfluxDBClient;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.shared.influx.DatabaseProperties;
import at.pegelhub.shared.influx.FluxDuration;
import at.pegelhub.testsupport.InfluxIntegrationTestBase;
import at.pegelhub.testsupport.PegelHubInfluxContainer;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import com.influxdb.exceptions.InfluxException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class InfluxMeasurementRepositoryTest extends InfluxIntegrationTestBase {

    private static final DatabaseProperties PROPERTIES = new DatabaseProperties(
            "url",
            PegelHubInfluxContainer.ORG,
            PegelHubInfluxContainer.DATA_BUCKET,
            PegelHubInfluxContainer.ADMIN_TOKEN);
    private static final FluxDuration LATEST_RANGE = new FluxDuration("72h");

    private InfluxDBClient client;
    private InfluxMeasurementRepository repository;

    @BeforeEach
    void setUp() {
        client = getInfluxDBDataClient();
        repository = new InfluxMeasurementRepository(client, PROPERTIES, LATEST_RANGE);
    }

    @AfterEach
    void tearDown() {
        client.close();
    }

    @Test
    void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new InfluxMeasurementRepository(null, PROPERTIES, LATEST_RANGE));
        assertThrows(NullPointerException.class, () -> new InfluxMeasurementRepository(client, null, LATEST_RANGE));
        assertThrows(NullPointerException.class, () -> new InfluxMeasurementRepository(client, PROPERTIES, null));
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

        assertThat(repository.getByTimeSeriesIdAndRange(timeSeriesId, "3h")).containsExactly(recentMeasurement);
        assertThat(repository.getLatestByTimeSeriesId(timeSeriesId)).isEqualTo(recentMeasurement);
    }

    @Test
    void invalidRangeThrowsIllegalArgumentException() {
        TimeSeriesId anyTimeSeries = new TimeSeriesId(UUID.randomUUID());
        assertThrows(IllegalArgumentException.class, () -> repository.getByTimeSeriesIdAndRange(anyTimeSeries, null));
        assertThrows(IllegalArgumentException.class, () -> repository.getByTimeSeriesIdAndRange(anyTimeSeries, ""));
        assertThrows(IllegalArgumentException.class, () -> repository.getByTimeSeriesIdAndRange(anyTimeSeries, "null"));
        assertThrows(IllegalArgumentException.class, () -> repository.getByTimeSeriesIdAndRange(anyTimeSeries, "-3d"));
    }

    @Test
    void missingLatestMeasurementThrowsInfluxException() {
        UUID id = UUID.fromString("e27efad9-b947-48b1-928e-c25663597f1c");

        assertThrows(InfluxException.class, () -> repository.getLatestByTimeSeriesId(new TimeSeriesId(id)));
    }
}
