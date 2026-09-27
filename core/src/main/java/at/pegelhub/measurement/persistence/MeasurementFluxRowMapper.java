package at.pegelhub.measurement.persistence;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measurement.application.MeasurementReadRow;
import at.pegelhub.measurement.application.LatestMeasurement;
import com.influxdb.exceptions.InfluxException;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
final class MeasurementFluxRowMapper {

    List<MeasurementReadRow> rawMeasurementRows(List<FluxTable> tables) {
        List<MeasurementReadRow> measurements = new ArrayList<>();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                measurements.add(rawMeasurementRow(record));
            }
        }
        return measurements;
    }

    MeasurementReadRow rawMeasurementRow(FluxRecord record) {
        return new MeasurementReadRow(
                requiredInstant(record, "_time"),
                requiredNumber(record, InfluxMeasurementSchema.VALUE_FIELD).doubleValue(),
                new ConnectorId(UUID.fromString(requiredString(
                        record, InfluxMeasurementSchema.SUBMITTED_BY_CONNECTOR_ID_TAG))));
    }

    List<LatestMeasurement> latestMeasurementRows(List<FluxTable> tables) {
        List<LatestMeasurement> measurements = new ArrayList<>();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                measurements.add(new LatestMeasurement(
                        new at.pegelhub.timeseries.domain.TimeSeriesId(UUID.fromString(
                                requiredString(record, "_measurement"))),
                        requiredInstant(record, "_time"),
                        requiredNumber(record, InfluxMeasurementSchema.VALUE_FIELD).doubleValue()));
            }
        }
        return measurements;
    }

    Instant systemTime(List<FluxTable> tables) {
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                return requiredInstant(record, "time");
            }
        }
        throw new InfluxException("InfluxDB did not return system time");
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
