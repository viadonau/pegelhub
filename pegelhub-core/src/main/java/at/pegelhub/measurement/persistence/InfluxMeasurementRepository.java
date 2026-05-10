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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static at.pegelhub.shared.influx.ConnectionHelper.AGGREGATE_RESULT_KEY;
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

    public InfluxMeasurementRepository(
            @Qualifier("dataClient") InfluxDBClient client,
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
            Point measurementData = Point.measurement(measurement.measurement().toString()).time((measurement.timestamp().toInstant(ZoneOffset.UTC)), WritePrecision.MS);
            for (Map.Entry<String, String> entry : measurement.infos().entrySet()) {
                measurementData.addTag(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, Double> entry : measurement.fields().entrySet()) {
                measurementData.addField(entry.getKey(), entry.getValue());
            }
            dataPoints.add(measurementData);
        }
        ConnectionHelper.writePoints(this.client, dataPoints);
    }

    /**
     * @param range in which the returned values reside.
     * @return the values inside the specified range
     */
    @Override
    public List<Measurement> getByRange(String range) {
        getSystemTime();
        String query = "from(bucket: \"" + properties.bucket() + "\") |> range(start: -" + range + ")";

        HashMap<String, HashMap<String, HashMap<String, Object>>> data = ConnectionHelper.queryData(this.client, query);
        return toMeasurement(data);
    }

    /**
     * @param id of the measurement.
     * @param range in which the returned values reside.
     * @return the value with the specified ID in the specified range
     */
    @Override
    public List<Measurement> getByIDAndRange(UUID id, String range) {
        String query = "from(bucket: \"" + properties.bucket() + "\") |> range(start: -" + range
                + ") |> filter(fn: (r) => r._measurement == \"" + id + "\")";

        HashMap<String, HashMap<String, HashMap<String, Object>>> data = ConnectionHelper.queryData(this.client, query);
        return toMeasurement(data);
    }

    /**
     *
     * @param uuid of the measurement.
     * @return the corresponding value to the specified {@link UUID}
     */
    @Override
    public Measurement getLastData(UUID uuid) {
        String query = "from(bucket: \"" + properties.bucket() + "\") |> range(start: -72h) |> filter(fn: (r) => r._measurement == \"" + uuid + "\") |> last()";
        List<Measurement> measurements = toMeasurement(ConnectionHelper.queryData(this.client, query));
        if (measurements.isEmpty()) {
            throw new InfluxException("No measurement found");
        }
        return measurements.stream()
                .max(Comparator.comparing(Measurement::timestamp))
                .orElseThrow(() -> new InfluxException("No measurement found"));
    }

    @Override
    public Measurement getAverageByIdAndRange(UUID id, String range) {
        String query = String.format(
                "from(bucket: \"%s\") |> range(start: -%s) |> filter(fn: (r) => r._measurement == \"%s\") |> mean()",
                properties.bucket(), range, id
        );

        HashMap<String, HashMap<String, HashMap<String, Object>>> data = ConnectionHelper.queryData(this.client, query);
        List<Measurement> results = toMeasurement(data);

        if (results.isEmpty()) {
            throw new NotFoundException(String.format("No data found for ID %s in the last %s to calculate an average.", id, range));
        }

        Measurement averagedMeasurement = results.getFirst();
        return new Measurement(
                averagedMeasurement.measurement(),
                LocalDateTime.now(ZoneOffset.UTC),
                averagedMeasurement.fields(),
                averagedMeasurement.infos()
        );
    }

    /**
     * @param data the data to be converted to measurements
     * @return the converted measurements
     */
    private List<Measurement> toMeasurement(HashMap<String, HashMap<String, HashMap<String, Object>>> data) {
        List<Measurement> measurements = new ArrayList<>();
        for (Map.Entry<String, HashMap<String, HashMap<String, Object>>> measurement : data.entrySet()) {
            for (Map.Entry<String, HashMap<String, Object>> measurementEntry : measurement.getValue().entrySet()) {
                HashMap<String, Object> measurementData = measurementEntry.getValue();
                HashMap<String, Double> fields = new HashMap<>(measurementData.entrySet().stream()
                        .filter(a -> a.getValue() instanceof Double)
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> (double) e.getValue())));
                HashMap<String, String> infos = new HashMap<>(measurementData.entrySet().stream()
                        .filter(a -> a.getValue() instanceof String)
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> (String) e.getValue())));

                LocalDateTime timestamp;
                if (measurementEntry.getKey().equals(AGGREGATE_RESULT_KEY)) {
                    timestamp = LocalDateTime.now(ZoneOffset.UTC);
                } else {
                    timestamp = LocalDateTime.ofInstant(Instant.parse(measurementEntry.getKey()), ZoneOffset.UTC);
                }

                measurements.add(new Measurement(
                        UUID.fromString(measurement.getKey()),
                        timestamp,
                        fields,
                        infos)
                );
            }
        }
        return measurements;
    }

    @Override
    public Timestamp getSystemTime() {
        String time = "import \"system\" import \"array\" array.from(rows: [{time: system.time()}])";

        QueryApi queryApi = this.client.getQueryApi();
        List<FluxTable> tables = queryApi.query(time);

        Instant returnResult = null;

        for (FluxTable table : tables) {
            List<FluxRecord> records = table.getRecords();
            for (FluxRecord record : records) {
                returnResult = (Instant) record.getValueByIndex(2);
            }
        }
        assert returnResult != null;
        return Timestamp.from(returnResult);
    }
}
