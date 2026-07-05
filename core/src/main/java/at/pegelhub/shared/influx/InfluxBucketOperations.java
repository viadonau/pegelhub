package at.pegelhub.shared.influx;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxTable;

import java.util.List;

import static at.pegelhub.shared.validation.Validations.requireNotEmpty;
import static java.util.Objects.requireNonNull;

/**
 * Operations against one configured Influx bucket.
 */
public final class InfluxBucketOperations {

    private final InfluxDBClient client;
    private final DatabaseProperties database;

    public InfluxBucketOperations(InfluxDBClient client, DatabaseProperties database) {
        this.client = requireNonNull(client);
        this.database = requireNonNull(database);
    }

    public String bucketName() {
        return database.bucket();
    }

    public void writePoints(List<Point> points) {
        requireNonNull(points);
        WriteApiBlocking writeApi = client.getWriteApiBlocking();
        writeApi.writePoints(database.bucket(), database.org(), points);
    }

    public void writePoint(Point point) {
        requireNonNull(point);
        WriteApiBlocking writeApi = client.getWriteApiBlocking();
        writeApi.writePoint(database.bucket(), database.org(), point);
    }

    public List<FluxTable> query(String flux) {
        requireNonNull(flux);
        return client.getQueryApi().query(flux, database.org());
    }

    public void validateReadable() {
        query(bucketReadCheck());
    }

    private String bucketReadCheck() {
        return "from(bucket: " + stringLiteral(database.bucket()) + ") |> range(start: -1s) |> limit(n: 1)";
    }

    private String stringLiteral(String value) {
        requireNotEmpty(value);
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
