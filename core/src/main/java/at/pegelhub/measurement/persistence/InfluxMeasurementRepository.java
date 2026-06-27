package at.pegelhub.measurement.persistence;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.exceptions.InfluxException;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measurement.application.MeasurementBucketQuery;
import at.pegelhub.measurement.application.MeasurementListQuery;
import at.pegelhub.measurement.application.MeasurementPageRow;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.measurement.domain.MeasurementBucket;
import at.pegelhub.shared.influx.DatabaseProperties;
import at.pegelhub.shared.influx.ConnectionHelper;
import at.pegelhub.shared.influx.FluxDuration;
import at.pegelhub.shared.influx.FluxQueries;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Influx implementation for {@code MeasurementRepository}.
 * Implements the storing/adding of data to the time series database.
 * Needs to be rewritten if time series database is going to be exchanged.
 */
@Repository
public class InfluxMeasurementRepository implements MeasurementRepository {

    private static final String VALUE_FIELD = "value";
    private static final String RECEIVED_AT_FIELD = "receivedAt";
    private static final String SUBMITTED_BY_CONNECTOR_ID_TAG = "submittedByConnectorId";

    private final InfluxDBClient client;
    private final DatabaseProperties properties;

    public InfluxMeasurementRepository(
            @Qualifier("influxDBClient") InfluxDBClient client,
            @Qualifier("dataConfiguration") DatabaseProperties properties) {
        this.client = requireNonNull(client);
        this.properties = requireNonNull(properties);
    }

    /**
     * @param measurements to save.
     */
    @Override
    public void storeMeasurements(List<Measurement> measurements) {
        List<Point> dataPoints = new ArrayList<>(measurements.size());
        for (Measurement measurement : measurements) {
            Point measurementData = Point.measurement(measurement.timeSeriesId().value().toString())
                    .time(measurement.observedAt(), WritePrecision.MS)
                    .addTag(SUBMITTED_BY_CONNECTOR_ID_TAG, measurement.submittedByConnectorId().value().toString())
                    .addField(VALUE_FIELD, measurement.value())
                    .addField(RECEIVED_AT_FIELD, measurement.receivedAt().toString());
            dataPoints.add(measurementData);
        }
        ConnectionHelper.writePoints(this.client, properties, dataPoints);
    }

    @Override
    public List<MeasurementPageRow> findMeasurements(MeasurementListQuery measurementQuery) {
        String query = FluxQueries.measurementPage(
                properties,
                measurementQuery.timeSeriesId().value(),
                measurementQuery.window().from(),
                measurementQuery.window().to(),
                measurementQuery.order(),
                measurementQuery.limit() + 1,
                measurementQuery.cursor());
        return toMeasurementPageRows(client.getQueryApi().query(query, properties.org()));
    }

    @Override
    public List<MeasurementBucket> findMeasurementBuckets(MeasurementBucketQuery query) {
        Duration bucketDuration = query.resolution().bucketWidth().duration();
        FluxDuration fluxBucket = FluxDuration.from(bucketDuration);
        Map<BucketKey, Double> means = aggregateValues(FluxQueries.meanMeasurementBuckets(
                properties,
                query.timeSeriesId().value(),
                query.window().from(),
                query.window().to(),
                fluxBucket), bucketDuration);
        Map<BucketKey, Long> counts = aggregateCounts(FluxQueries.countMeasurementBuckets(
                properties,
                query.timeSeriesId().value(),
                query.window().from(),
                query.window().to(),
                fluxBucket), bucketDuration);

        return means.entrySet().stream()
                .filter(entry -> counts.containsKey(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new MeasurementBucket(
                        query.timeSeriesId(),
                        entry.getKey().from(),
                        entry.getKey().to(),
                        entry.getValue(),
                        counts.get(entry.getKey())))
                .toList();
    }

    private Map<BucketKey, Double> aggregateValues(String query, Duration bucketDuration) {
        List<FluxTable> tables = client.getQueryApi().query(query, properties.org());
        Map<BucketKey, Double> values = new HashMap<>();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                values.put(bucketKey(record, bucketDuration), aggregateNumber(record).doubleValue());
            }
        }
        return values;
    }

    private Map<BucketKey, Long> aggregateCounts(String query, Duration bucketDuration) {
        List<FluxTable> tables = client.getQueryApi().query(query, properties.org());
        Map<BucketKey, Long> counts = new HashMap<>();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                counts.put(bucketKey(record, bucketDuration), aggregateNumber(record).longValue());
            }
        }
        return counts;
    }

    private BucketKey bucketKey(FluxRecord record, Duration bucketDuration) {
        Instant from = aggregateInstant(record, "_time");
        return new BucketKey(from, from.plus(bucketDuration));
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

    /**
     * @return the converted measurement page rows
     */
    private List<MeasurementPageRow> toMeasurementPageRows(List<FluxTable> tables) {
        List<MeasurementPageRow> measurements = new ArrayList<>();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                measurements.add(new MeasurementPageRow(
                        requiredInstant(record, "_time"),
                        requiredNumber(record, VALUE_FIELD).doubleValue(),
                        new ConnectorId(UUID.fromString(requiredString(record, SUBMITTED_BY_CONNECTOR_ID_TAG))))
                );
            }
        }
        return measurements;
    }

    private static Instant requiredInstant(FluxRecord record, String column) {
        Object value = record.getValueByKey(column);
        if (value instanceof Instant instant) {
            return instant;
        }
        throw new InfluxException("Measurement page row is missing instant column " + column);
    }

    private static String requiredString(FluxRecord record, String column) {
        Object value = record.getValueByKey(column);
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        throw new InfluxException("Measurement page row is missing string column " + column);
    }

    private static Number requiredNumber(FluxRecord record, String column) {
        Object value = record.getValueByKey(column);
        if (value instanceof Number number) {
            return number;
        }
        throw new InfluxException("Measurement page row is missing numeric column " + column);
    }

    private record BucketKey(Instant from, Instant to) implements Comparable<BucketKey> {

        @Override
        public int compareTo(BucketKey other) {
            int fromComparison = from.compareTo(other.from);
            return fromComparison != 0 ? fromComparison : to.compareTo(other.to);
        }
    }

    @Override
    public Instant getSystemTime() {
        List<FluxTable> tables = client.getQueryApi().query(FluxQueries.systemTime(), properties.org());
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                return requiredInstant(record, "time");
            }
        }
        throw new InfluxException("InfluxDB did not return system time");
    }
}
