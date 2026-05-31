package at.pegelhub.shared.influx;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Class, which handles the basic communication with the influxDB.
 * Needs to be rewritten if the time series database is to be exchanged
 */
public final class ConnectionHelper {
    private ConnectionHelper() {
        throw new IllegalStateException("utility class can not be initialized.");
    }

    public static void writePoints(InfluxDBClient client, DatabaseProperties database, List<Point> dataPoints) {
        requireNonNull(client);
        requireNonNull(database);
        requireNonNull(dataPoints);
        WriteApiBlocking writeApi = client.getWriteApiBlocking();
        writeApi.writePoints(database.bucket(), database.org(), dataPoints);
    }

    public static void writePoint(InfluxDBClient client, DatabaseProperties database, Point dataPoint) {
        requireNonNull(client);
        requireNonNull(database);
        requireNonNull(dataPoint);
        WriteApiBlocking writeApi = client.getWriteApiBlocking();
        writeApi.writePoint(database.bucket(), database.org(), dataPoint);
    }

    public static List<InfluxPoint> queryData(
            InfluxDBClient client,
            DatabaseProperties database,
            String query) {
        requireNonNull(client);
        requireNonNull(database);
        requireNonNull(query);
        List<FluxTable> tables = client.getQueryApi().query(query, database.org());
        Map<PointKey, MutableInfluxPoint> points = new LinkedHashMap<>();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                String measurement = requireNonNull(record.getMeasurement());
                Instant timestamp = record.getTime();
                MutableInfluxPoint point = points.computeIfAbsent(
                        new PointKey(measurement, timestamp),
                        ignored -> new MutableInfluxPoint(measurement, timestamp));

                if (record.getField() != null) {
                    point.fields().put(record.getField(), record.getValue());
                }

                for (Map.Entry<String, Object> valueEntry : record.getValues().entrySet()) {
                    if (isUserTagColumn(valueEntry)) {
                        point.tags().put(valueEntry.getKey(), (String) valueEntry.getValue());
                    }
                }
            }
        }

        return points.values().stream()
                .map(MutableInfluxPoint::toInfluxPoint)
                .toList();
    }

    public static void validateBucketReadable(InfluxDBClient client, DatabaseProperties database) {
        requireNonNull(client);
        requireNonNull(database);
        client.getQueryApi().query(FluxQueries.bucketReadCheck(database), database.org());
    }

    private static boolean isUserTagColumn(Map.Entry<String, Object> valueEntry) {
        String columnName = valueEntry.getKey();
        return valueEntry.getValue() instanceof String
                && !columnName.startsWith("_")
                && !"result".equals(columnName)
                && !"table".equals(columnName);
    }

    private record PointKey(String measurement, Instant timestamp) {
    }

    private record MutableInfluxPoint(
            String measurement,
            Instant timestamp,
            Map<String, Object> fields,
            Map<String, String> tags) {

        private MutableInfluxPoint(String measurement, Instant timestamp) {
            this(measurement, timestamp, new HashMap<>(), new HashMap<>());
        }

        private InfluxPoint toInfluxPoint() {
            return new InfluxPoint(measurement, timestamp, fields, tags);
        }
    }
}
