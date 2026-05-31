package at.pegelhub.measurement.persistence;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.exceptions.InfluxException;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.shared.influx.DatabaseProperties;
import at.pegelhub.shared.influx.ConnectionHelper;
import at.pegelhub.shared.influx.FluxDuration;
import at.pegelhub.shared.influx.FluxQueries;
import at.pegelhub.shared.influx.InfluxPoint;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Influx implementation for {@code MeasurementRepository}.
 * Implements the storing/adding of data to the time series database.
 * Needs to be rewritten if time series database is going to be exchanged.
 */
@Repository
public class InfluxMeasurementRepository implements MeasurementRepository {

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
            Point measurementData = Point.measurement(measurement.measurement().toString())
                    .time(measurement.timestamp(), WritePrecision.MS);
            for (Map.Entry<String, String> entry : measurement.infos().entrySet()) {
                measurementData.addTag(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, Double> entry : measurement.fields().entrySet()) {
                measurementData.addField(entry.getKey(), entry.getValue());
            }
            dataPoints.add(measurementData);
        }
        ConnectionHelper.writePoints(this.client, properties, dataPoints);
    }

    /**
     * @param range in which the returned values reside.
     * @return the values inside the specified range
     */
    @Override
    public List<Measurement> getByRange(String range) {
        String query = FluxQueries.range(properties, new FluxDuration(range));

        return toMeasurements(ConnectionHelper.queryData(this.client, properties, query));
    }

    /**
     * @param id of the measurement.
     * @param range in which the returned values reside.
     * @return the value with the specified ID in the specified range
     */
    @Override
    public List<Measurement> getByIDAndRange(UUID id, String range) {
        String query = FluxQueries.measurementRange(properties, id, new FluxDuration(range));

        return toMeasurements(ConnectionHelper.queryData(this.client, properties, query));
    }

    /**
     *
     * @param uuid of the measurement.
     * @return the corresponding value to the specified {@link UUID}
     */
    @Override
    public Measurement getLastData(UUID uuid) {
        String query = FluxQueries.latestMeasurement(properties, uuid, latestRange);
        List<Measurement> measurements = toMeasurements(ConnectionHelper.queryData(this.client, properties, query));
        if (measurements.isEmpty()) {
            throw new InfluxException("No measurement found");
        }
        return measurements.stream()
                .max(Comparator.comparing(Measurement::timestamp))
                .orElseThrow(() -> new InfluxException("No measurement found"));
    }

    @Override
    public Measurement getAverageByIdAndRange(UUID id, String range) {
        String query = FluxQueries.meanMeasurement(properties, id, new FluxDuration(range));

        List<Measurement> results = toMeasurements(ConnectionHelper.queryData(this.client, properties, query));

        if (results.isEmpty()) {
            throw new NotFoundException(String.format("No data found for ID %s in the last %s to calculate an average.", id, range));
        }

        Measurement averagedMeasurement = results.getFirst();
        return new Measurement(
                averagedMeasurement.measurement(),
                Instant.now(),
                averagedMeasurement.fields(),
                averagedMeasurement.infos()
        );
    }

    /**
     * @param data the data to be converted to measurements
     * @return the converted measurements
     */
    private List<Measurement> toMeasurements(List<InfluxPoint> data) {
        List<Measurement> measurements = new ArrayList<>();
        for (InfluxPoint point : data) {
            HashMap<String, Double> fields = new HashMap<>(point.fields().entrySet().stream()
                    .filter(entry -> entry.getValue() instanceof Number)
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> ((Number) entry.getValue()).doubleValue())));
            HashMap<String, String> infos = new HashMap<>(point.tags());
            Instant timestamp = point.isAggregate()
                    ? Instant.now()
                    : point.timestamp();

            measurements.add(new Measurement(
                    UUID.fromString(point.measurement()),
                    timestamp,
                    fields,
                    infos)
            );
        }
        return measurements;
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
