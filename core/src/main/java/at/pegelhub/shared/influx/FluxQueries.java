package at.pegelhub.shared.influx;

import java.util.UUID;

import static at.pegelhub.shared.validation.Validations.requireNotEmpty;
import static java.util.Objects.requireNonNull;

/**
 * Small Flux query builder for the Influx repository queries used by PegelHub.
 */
public final class FluxQueries {

    private FluxQueries() {
        throw new IllegalStateException("utility class can not be initialized.");
    }

    public static String range(DatabaseProperties database, FluxDuration range) {
        requireNonNull(range);
        return from(database) + " |> range(start: " + range.negativeLiteral() + ")";
    }

    public static String measurementRange(DatabaseProperties database, UUID measurement, FluxDuration range) {
        return range(database, range) + measurementFilter(measurement);
    }

    public static String latestMeasurement(DatabaseProperties database, UUID measurement, FluxDuration latestRange) {
        return measurementRange(database, measurement, latestRange) + " |> last()";
    }

    public static String meanMeasurement(DatabaseProperties database, UUID measurement, FluxDuration range) {
        return measurementRange(database, measurement, range) + valueFieldFilter() + " |> mean()";
    }

    public static String bucketReadCheck(DatabaseProperties database) {
        return from(database) + " |> range(start: -1s) |> limit(n: 1)";
    }

    private static String from(DatabaseProperties database) {
        requireNonNull(database);
        return "from(bucket: " + stringLiteral(database.bucket()) + ")";
    }

    private static String measurementFilter(UUID measurement) {
        requireNonNull(measurement);
        return " |> filter(fn: (r) => r._measurement == " + stringLiteral(measurement.toString()) + ")";
    }

    private static String valueFieldFilter() {
        return " |> filter(fn: (r) => r._field == \"value\")";
    }

    static String stringLiteral(String value) {
        requireNotEmpty(value);
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
