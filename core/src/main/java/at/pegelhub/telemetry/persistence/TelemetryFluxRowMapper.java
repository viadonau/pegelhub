package at.pegelhub.telemetry.persistence;

import at.pegelhub.telemetry.domain.Telemetry;
import com.influxdb.exceptions.InfluxException;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@Component
final class TelemetryFluxRowMapper {

    /*
     * Refactor candidate: this mapper preserves the current legacy Telemetry shape.
     * Revisit it when connector runtime telemetry is modeled deliberately.
     */

    private static final String STATION_IP_ADDRESS_INTERN = "stationIPAddressIntern";
    private static final String STATION_IP_ADDRESS_EXTERN = "stationIPAddressExtern";
    private static final String CYCLE_TIME = "cycleTime";
    private static final String TEMPERATURE_WATER = "temperatureWater";
    private static final String TEMPERATURE_AIR = "temperatureAir";
    private static final String PERFORMANCE_VOLTAGE_BATTERY = "performanceVoltageBattery";
    private static final String PERFORMANCE_VOLTAGE_SUPPLY = "performanceVoltageSupply";
    private static final String PERFORMANCE_ELECTRICITY_BATTERY = "performanceElectricityBattery";
    private static final String PERFORMANCE_ELECTRICITY_SUPPLY = "performanceElectricitySupply";
    private static final String FIELD_STRENGTH_TRANSMISSION = "fieldStrengthTransmission";

    List<Telemetry> toTelemetries(List<FluxTable> tables) {
        requireNonNull(tables);
        // Flux returns one row per field; rebuild each Telemetry point by merging
        // rows with the same connector identifier and timestamp.
        Map<TelemetryPointKey, MutableTelemetryPoint> points = new LinkedHashMap<>();
        for (FluxTable table : tables) {
            mergeTableRows(table, points);
        }
        return toTelemetries(points);
    }

    // Table rows [cycleTime, temperatureWater] --> both rows merged into the point map.
    private void mergeTableRows(FluxTable table, Map<TelemetryPointKey, MutableTelemetryPoint> points) {
        requireNonNull(table);
        for (FluxRecord record : table.getRecords()) {
            mergeRow(record, points);
        }
    }

    // Row(field=cycleTime, value=30, stationIPAddressIntern=a) --> field and address copied to one point.
    private void mergeRow(FluxRecord record, Map<TelemetryPointKey, MutableTelemetryPoint> points) {
        MutableTelemetryPoint point = pointFor(record, points);
        copyFieldValue(record, point);
        copyStationIpAddresses(record, point);
    }

    // Row(measurement=connector-a, time=t) --> existing or new Telemetry point for (connector-a, t).
    private MutableTelemetryPoint pointFor(FluxRecord record, Map<TelemetryPointKey, MutableTelemetryPoint> points) {
        TelemetryPointKey key = pointKey(record);
        return points.computeIfAbsent(
                key,
                ignored -> new MutableTelemetryPoint(key.stationIdentifier(), key.timestamp()));
    }

    // Connector measurement/time columns --> key used to join rows from the same Telemetry point.
    private TelemetryPointKey pointKey(FluxRecord record) {
        requireNonNull(record);
        Instant timestamp = record.getTime();
        if (timestamp == null) {
            throw new InfluxException("Telemetry query returned a point without a timestamp");
        }
        return new TelemetryPointKey(requireNonNull(record.getMeasurement()), timestamp);
    }

    // Row(_field=cycleTime, _value=30) --> fields[cycleTime]=30.
    private void copyFieldValue(FluxRecord record, MutableTelemetryPoint point) {
        if (record.getField() != null) {
            point.fields().put(record.getField(), record.getValue());
        }
    }

    // Row(stationIPAddressIntern=a) --> tags[stationIPAddressIntern]=a.
    private void copyStationIpAddresses(FluxRecord record, MutableTelemetryPoint point) {
        copyStringColumn(record, point, STATION_IP_ADDRESS_INTERN);
        copyStringColumn(record, point, STATION_IP_ADDRESS_EXTERN);
    }

    // Column stationIPAddressIntern=a --> tags[stationIPAddressIntern]=a; missing/non-string values are skipped.
    private void copyStringColumn(FluxRecord record, MutableTelemetryPoint point, String column) {
        Object value = record.getValueByKey(column);
        if (value instanceof String text) {
            point.tags().put(column, text);
        }
    }

    // Mutable point map --> Telemetry list in first-seen order.
    private List<Telemetry> toTelemetries(Map<TelemetryPointKey, MutableTelemetryPoint> points) {
        return points.values().stream()
                .map(MutableTelemetryPoint::toTelemetry)
                .toList();
    }

    private static Integer toInt(Object value) {
        return Math.toIntExact(((Number) value).longValue());
    }

    private static Double toDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private record TelemetryPointKey(String stationIdentifier, Instant timestamp) {
    }

    private record MutableTelemetryPoint(
            String stationIdentifier,
            Instant timestamp,
            Map<String, Object> fields,
            Map<String, String> tags) {

        private MutableTelemetryPoint(String stationIdentifier, Instant timestamp) {
            this(stationIdentifier, timestamp, new HashMap<>(), new HashMap<>());
        }

        private Telemetry toTelemetry() {
            return new Telemetry(
                    stationIdentifier,
                    tags.get(STATION_IP_ADDRESS_INTERN),
                    tags.get(STATION_IP_ADDRESS_EXTERN),
                    timestamp,
                    toInt(fields.get(CYCLE_TIME)),
                    toDouble(fields.get(TEMPERATURE_WATER)),
                    toDouble(fields.get(TEMPERATURE_AIR)),
                    toDouble(fields.get(PERFORMANCE_VOLTAGE_BATTERY)),
                    toDouble(fields.get(PERFORMANCE_VOLTAGE_SUPPLY)),
                    toDouble(fields.get(PERFORMANCE_ELECTRICITY_BATTERY)),
                    toDouble(fields.get(PERFORMANCE_ELECTRICITY_SUPPLY)),
                    toDouble(fields.get(FIELD_STRENGTH_TRANSMISSION)));
        }
    }
}
