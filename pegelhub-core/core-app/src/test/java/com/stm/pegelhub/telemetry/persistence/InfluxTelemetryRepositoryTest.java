package com.stm.pegelhub.outbound.data;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.exceptions.InfluxException;
import com.stm.pegelhub.testsupport.InfluxIntegrationTestBase;
import com.stm.pegelhub.common.model.data.Telemetry;
import com.stm.pegelhub.outbound.db.DatabaseProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class InfluxTelemetryRepositoryTest extends InfluxIntegrationTestBase {


    private static final DatabaseProperties PROPERTIES = new DatabaseProperties("url", "org", "telemetry", "token");
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
    public void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new InfluxTelemetryRepository(null, PROPERTIES));
        assertThrows(NullPointerException.class, () -> new InfluxTelemetryRepository(client, null));
    }

    @Test
    void saveTelemetryThrowsNPE() {
        assertThrows(NullPointerException.class, () -> repository.saveTelemetry(null));
    }

    @Test
    void saveTelemetry() {
        String id = "0d0f370f-dc56-472a-a895-1547ce0ed43c";
        Telemetry Telemetry = new Telemetry(
                id,
                "255.255.255.0",
                "255.255.255.0",
                Instant.now()
                        .minus(48, ChronoUnit.HOURS)
                        .truncatedTo(ChronoUnit.SECONDS).toString(),
                10, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0);
        Telemetry savedTelemetry = repository.saveTelemetry(Telemetry);

        Telemetry queryTelemetry = repository.getLastData(UUID.fromString(id));
        assertEquals(Telemetry, savedTelemetry);
        assertEquals(Telemetry, queryTelemetry);
    }

    @Test
    void getByRangeWithInvalidValueThrowsIE() {
        assertThrows(InfluxException.class, () -> repository.getByRange(null));
        assertThrows(InfluxException.class, () -> repository.getByRange(""));
        assertThrows(InfluxException.class, () -> repository.getByRange("null"));
        assertThrows(InfluxException.class, () -> repository.getByRange("-3d"));
    }

    @Test
    void getByRange() {
        String range = "40h";
        List<Telemetry> Telemetrys = repository.getByRange(range);
        assertEquals(2, Telemetrys.size());
    }

    @Test
    void getByShortRange() {
        String range = "16h";
        List<Telemetry> Telemetrys = repository.getByRange(range);
        assertEquals(1, Telemetrys.size());
    }

    @Test
    void getLastDataWithWrongUUIDThrowsIE() {
        UUID uuid = UUID.fromString("e27efad9-b947-48b1-928e-c25663597f1c");
        assertThrows(InfluxException.class, () -> repository.getLastData(uuid));
    }

    @Test
    void getLastData() {
        String id = "e27efad2-b947-48b1-928e-c25663597f1c";
        Telemetry lastData = repository.getLastData(UUID.fromString(id));
        assertEquals(id, lastData.measurement());
        assertEquals(11, lastData.cycleTime());
    }
}
