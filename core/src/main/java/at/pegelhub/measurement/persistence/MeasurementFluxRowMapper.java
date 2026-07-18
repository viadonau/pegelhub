package at.pegelhub.measurement.persistence;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measurement.application.MeasurementReadRow;
import com.influxdb.exceptions.InfluxException;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
final class MeasurementFluxRowMapper {

    List<MeasurementReadRow> rawMeasurementRows(List<FluxTable> tables) {
        List<MeasurementReadRow> measurements = new ArrayList<>();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                measurements.add(new MeasurementReadRow(
                        requiredInstant(record, "_time"),
                        requiredNumber(record, InfluxMeasurementSchema.VALUE_FIELD).doubleValue(),
                        new ConnectorId(UUID.fromString(requiredString(
                                record,
                                InfluxMeasurementSchema.SUBMITTED_BY_CONNECTOR_ID_TAG))))
                );
            }
        }
        return measurements;
    }

    Map<MeasurementBucketKey, Double> meanRows(List<FluxTable> tables, Duration bucketDuration) {
        Map<MeasurementBucketKey, Double> values = new HashMap<>();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                values.put(bucketKey(record, bucketDuration), aggregateNumber(record).doubleValue());
            }
        }
        return values;
    }

    Map<MeasurementBucketKey, Long> countRows(List<FluxTable> tables, Duration bucketDuration) {
        Map<MeasurementBucketKey, Long> counts = new HashMap<>();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                counts.put(bucketKey(record, bucketDuration), aggregateNumber(record).longValue());
            }
        }
        return counts;
    }

    Instant systemTime(List<FluxTable> tables) {
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                return requiredInstant(record, "time");
            }
        }
        throw new InfluxException("InfluxDB did not return system time");
    }

    private MeasurementBucketKey bucketKey(FluxRecord record, Duration bucketDuration) {
        Instant from = aggregateInstant(record, "_time");
        return new MeasurementBucketKey(from, from.plus(bucketDuration));
    }

    private Instant aggregateInstant(FluxRecord record, String column) {
        Object value = record.getValueByKey(column);
        if (value instanceof Instant instant) {
            return instant;
        }
        throw new InfluxException("Measurement aggregate is missing timestamp column " + column);
    }

    private Number aggregateNumber(FluxRecord record) {
        Object value = record.getValue();
        if (value instanceof Number number) {
            return number;
        }
        throw new InfluxException("Measurement aggregate is missing numeric value");
    }

    private Instant requiredInstant(FluxRecord record, String column) {
        Object value = record.getValueByKey(column);
        if (value instanceof Instant instant) {
            return instant;
        }
        throw new InfluxException("Measurement read row is missing instant column " + column);
    }

    private String requiredString(FluxRecord record, String column) {
        Object value = record.getValueByKey(column);
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        throw new InfluxException("Measurement read row is missing string column " + column);
    }

    private Number requiredNumber(FluxRecord record, String column) {
        Object value = record.getValueByKey(column);
        if (value instanceof Number number) {
            return number;
        }
        throw new InfluxException("Measurement read row is missing numeric column " + column);
    }

}
