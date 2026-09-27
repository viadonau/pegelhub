package at.pegelhub.shared.influx;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxTable;
import com.influxdb.query.FluxRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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

    /** Streams records without Flux regrouping, aborting rather than returning a partial capped result. */
    public List<FluxRecord> queryBounded(String flux, int maxRecords) {
        requireNonNull(flux);
        if (maxRecords < 0) throw new IllegalArgumentException("maxRecords must not be negative");
        var result = new CompletableFuture<List<FluxRecord>>();
        var rows = new ArrayList<FluxRecord>();
        client.getQueryApi().query(flux, database.org(), (cancellable, record) -> {
            if (rows.size() == maxRecords) {
                result.completeExceptionally(new IllegalArgumentException(
                        "query exceeds " + maxRecords + " records"));
                cancellable.cancel();
            } else {
                rows.add(record);
            }
        }, result::completeExceptionally, () -> result.complete(List.copyOf(rows)));
        try {
            return result.join();
        } catch (CompletionException exception) {
            if (exception.getCause() instanceof RuntimeException cause) throw cause;
            throw exception;
        }
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
