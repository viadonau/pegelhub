package at.pegelhub.shared.influx;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxTable;

import java.util.List;
import java.time.Duration;

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

    public List<FluxTable> query(String flux, Duration timeout) {
        try (var bounded = boundedClient(timeout)) {
            return bounded.getQueryApi().query(flux, database.org());
        }
    }

    public void writePoints(List<Point> points, Duration timeout) {
        try (var bounded = boundedClient(timeout)) {
            bounded.getWriteApiBlocking().writePoints(database.bucket(), database.org(), points);
        }
    }

    private InfluxDBClient boundedClient(Duration timeout) {
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Timeout must be positive");
        }

        // Keep internal deadlines off the shared client used by ingestion and interactive reads.
        var http = new okhttp3.OkHttpClient.Builder()
                .callTimeout(timeout)
                .connectTimeout(timeout.compareTo(Duration.ofSeconds(10)) < 0 ? timeout : Duration.ofSeconds(10))
                .readTimeout(timeout)
                .writeTimeout(timeout);

        return InfluxDBClientFactory.create(InfluxDBClientOptions.builder()
                .url(database.url())
                .authenticateToken(database.token().toCharArray())
                .org(database.org())
                .okHttpClient(http)
                .build());
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
