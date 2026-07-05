package at.pegelhub.telemetry.persistence;

import at.pegelhub.telemetry.domain.Telemetry;
import com.influxdb.exceptions.InfluxException;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class TelemetryFluxRowMapperTest {

    private final TelemetryFluxRowMapper mapper = new TelemetryFluxRowMapper();

    @Test
    void mergesTelemetryFieldRowsIntoOneTelemetryPoint() {
        Instant timestamp = Instant.parse("2026-06-17T12:00:00Z");

        List<Telemetry> telemetries = mapper.toTelemetries(List.of(table(
                record("station-a", timestamp, "cycleTime", 30, Map.of(
                        "stationIPAddressIntern", "10.0.0.1",
                        "stationIPAddressExtern", "203.0.113.1")),
                record("station-a", timestamp, "temperatureWater", 14.2, Map.of(
                        "stationIPAddressIntern", "10.0.0.1",
                        "stationIPAddressExtern", "203.0.113.1")))));

        assertThat(telemetries)
                .singleElement()
                .satisfies(telemetry -> {
                    assertThat(telemetry.measurement()).isEqualTo("station-a");
                    assertThat(telemetry.timestamp()).isEqualTo(timestamp);
                    assertThat(telemetry.cycleTime()).isEqualTo(30);
                    assertThat(telemetry.temperatureWater()).isEqualTo(14.2);
                    assertThat(telemetry.stationIPAddressIntern()).isEqualTo("10.0.0.1");
                    assertThat(telemetry.stationIPAddressExtern()).isEqualTo("203.0.113.1");
                });
    }

    @Test
    void ignoresNonTelemetryStringColumns() {
        Instant timestamp = Instant.parse("2026-06-17T12:00:00Z");

        List<Telemetry> telemetries = mapper.toTelemetries(List.of(table(
                record("station-a", timestamp, "cycleTime", 30, Map.of(
                        "host", "connector-1",
                        "stationIPAddressIntern", "10.0.0.1",
                        "stationIPAddressExtern", "203.0.113.1")))));

        assertThat(telemetries)
                .singleElement()
                .satisfies(telemetry -> {
                    assertThat(telemetry.stationIPAddressIntern()).isEqualTo("10.0.0.1");
                    assertThat(telemetry.stationIPAddressExtern()).isEqualTo("203.0.113.1");
                });
    }

    @Test
    void rejectsTelemetryRowsWithoutTimestamp() {
        assertThrows(InfluxException.class, () -> mapper.toTelemetries(List.of(table(
                record("station-a", null, "cycleTime", 30, Map.of())))));
    }

    private static FluxTable table(FluxRecord... records) {
        FluxTable table = new FluxTable();
        table.getRecords().addAll(List.of(records));
        return table;
    }

    private static FluxRecord record(
            String measurement,
            Instant timestamp,
            String field,
            Object value,
            Map<String, Object> columns) {
        FluxRecord record = new FluxRecord(0);
        record.getValues().put("_measurement", measurement);
        record.getValues().put("_time", timestamp);
        record.getValues().put("_field", field);
        record.getValues().put("_value", value);
        record.getValues().putAll(columns);
        return record;
    }
}
