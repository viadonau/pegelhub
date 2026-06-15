package at.pegelhub.measurement.persistence;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.exceptions.InfluxException;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.measurement.domain.MeasurementAverage;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.shared.influx.DatabaseProperties;
import at.pegelhub.shared.influx.ConnectionHelper;
import at.pegelhub.shared.influx.FluxDuration;
import at.pegelhub.shared.influx.FluxQueries;
import at.pegelhub.shared.influx.InfluxPoint;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
    private final FluxDuration latestRange;

    public InfluxMeasurementRepository(
            @Qualifier("influxDBClient") InfluxDBClient client,
            @Qualifier("dataConfiguration") DatabaseProperties properties,
            @Qualifier("latestRange") FluxDuration latestRange) {
        this.client = requireNonNull(client);
        this.properties = requireNonNull(properties);
        this.latestRange = requireNonNull(latestRange);
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

    /**
     * @param timeSeriesId of the TimeSeries.
     * @param range in which the returned values reside.
     * @return the value with the specified ID in the specified range
     */
    @Override
    public List<Measurement> getByTimeSeriesIdAndRange(TimeSeriesId timeSeriesId, String range) {
        String query = FluxQueries.measurementRange(properties, timeSeriesId.value(), new FluxDuration(range));

        return toMeasurements(ConnectionHelper.queryData(this.client, properties, query));
    }

    @Override
    public Measurement getLatestByTimeSeriesId(TimeSeriesId timeSeriesId) {
        String query = FluxQueries.latestMeasurement(properties, timeSeriesId.value(), latestRange);
        List<Measurement> measurements = toMeasurements(ConnectionHelper.queryData(this.client, properties, query));
        if (measurements.isEmpty()) {
            throw new NotFoundException("No measurement found");
        }
        return measurements.stream()
                .max(Comparator.comparing(Measurement::observedAt))
                .orElseThrow(() -> new NotFoundException("No measurement found"));
    }

    @Override
    public MeasurementAverage getAverageByTimeSeriesIdAndRange(TimeSeriesId timeSeriesId, String range) {
        FluxDuration duration = new FluxDuration(range);
        FluxRecord mean = singleAggregateRecord(FluxQueries.meanMeasurement(properties, timeSeriesId.value(), duration), timeSeriesId, range);
        FluxRecord count = singleAggregateRecord(FluxQueries.countMeasurement(properties, timeSeriesId.value(), duration), timeSeriesId, range);

        return new MeasurementAverage(
                timeSeriesId,
                aggregateInstant(mean, "_start"),
                aggregateInstant(mean, "_stop"),
                aggregateNumber(mean).doubleValue(),
                aggregateNumber(count).longValue());
    }

    private FluxRecord singleAggregateRecord(String query, TimeSeriesId timeSeriesId, String range) {
        List<FluxTable> tables = client.getQueryApi().query(query, properties.org());
        return tables.stream()
                .flatMap(table -> table.getRecords().stream())
                .findFirst()
                .orElseThrow(() -> new NotFoundException(String.format(
                        "No data found for TimeSeries %s in the last %s to calculate an average.",
                        timeSeriesId.value(),
                        range)));
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
     * @param data the data to be converted to measurements
     * @return the converted measurements
     */
    private List<Measurement> toMeasurements(List<InfluxPoint> data) {
        List<Measurement> measurements = new ArrayList<>();
        for (InfluxPoint point : data) {
            Instant observedAt = point.isAggregate()
                    ? Instant.now()
                    : point.timestamp();
            Instant receivedAt = point.fields().containsKey(RECEIVED_AT_FIELD)
                    ? Instant.parse(point.fields().get(RECEIVED_AT_FIELD).toString())
                    : Instant.now();

            measurements.add(new Measurement(
                    new TimeSeriesId(UUID.fromString(point.measurement())),
                    observedAt,
                    receivedAt,
                    numericField(point, VALUE_FIELD),
                    new ConnectorId(UUID.fromString(point.tags().get(SUBMITTED_BY_CONNECTOR_ID_TAG))))
            );
        }
        return measurements;
    }

    private double numericField(InfluxPoint point, String field) {
        Object value = point.fields().get(field);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new InfluxException("Measurement point is missing numeric field " + field);
    }

    @Override
    public Instant getSystemTime() {
        String time = "import \"system\" import \"array\" array.from(rows: [{time: system.time()}])";

        QueryApi queryApi = this.client.getQueryApi();
        List<FluxTable> tables = queryApi.query(time, properties.org());

        Instant returnResult = null;

        for (FluxTable table : tables) {
            List<FluxRecord> records = table.getRecords();
            for (FluxRecord record : records) {
                returnResult = (Instant) record.getValueByIndex(2);
            }
        }
        if (returnResult == null) {
            throw new InfluxException("InfluxDB did not return system time");
        }
        return returnResult;
    }
}
