package at.pegelhub.shared.influx;

import at.pegelhub.measurement.application.MeasurementCursor;
import at.pegelhub.measurement.application.MeasurementOrder;

import java.time.Instant;
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

    public static String measurementWindow(DatabaseProperties database, UUID measurement, Instant from, Instant to) {
        requireNonNull(from);
        requireNonNull(to);
        if (!to.isAfter(from)) {
            throw new IllegalArgumentException("to must be after from");
        }
        return from(database)
                + " |> range(start: time(v: " + stringLiteral(from.toString()) + "), stop: time(v: " + stringLiteral(to.toString()) + "))"
                + measurementFilter(measurement);
    }

    public static String measurementPage(
            DatabaseProperties database,
            UUID measurement,
            Instant from,
            Instant to,
            MeasurementOrder order,
            int limit,
            MeasurementCursor cursor) {
        requireNonNull(order);
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }

        String measurementRows = measurementWindow(database, measurement, from, to)
                + cursorFilter(order, cursor);
        String pageOperations = " |> group(columns: [])"
                + sortByMeasurementPosition(order)
                + " |> limit(n: " + limit + ")";

        return measurementRows
                + valueFieldFilter()
                + pageOperations
                + " |> rename(columns: {_value: \"value\"})"
                + " |> keep(columns: [\"_time\", \"submittedByConnectorId\", \"value\"])";
    }

    public static String latestMeasurement(DatabaseProperties database, UUID measurement, FluxDuration latestRange) {
        return measurementRange(database, measurement, latestRange) + " |> last()";
    }

    public static String meanMeasurement(DatabaseProperties database, UUID measurement, FluxDuration range) {
        return measurementRange(database, measurement, range) + valueFieldFilter() + measurementGroup() + " |> mean()";
    }

    public static String countMeasurement(DatabaseProperties database, UUID measurement, FluxDuration range) {
        return measurementRange(database, measurement, range) + valueFieldFilter() + measurementGroup() + " |> count()";
    }

    public static String meanMeasurementBuckets(
            DatabaseProperties database,
            UUID measurement,
            Instant from,
            Instant to,
            FluxDuration bucket) {
        return measurementWindow(database, measurement, from, to)
                + valueFieldFilter()
                + measurementGroup()
                + aggregateWindow(bucket, "mean");
    }

    public static String countMeasurementBuckets(
            DatabaseProperties database,
            UUID measurement,
            Instant from,
            Instant to,
            FluxDuration bucket) {
        return measurementWindow(database, measurement, from, to)
                + valueFieldFilter()
                + measurementGroup()
                + aggregateWindow(bucket, "count");
    }

    public static String bucketReadCheck(DatabaseProperties database) {
        return from(database) + " |> range(start: -1s) |> limit(n: 1)";
    }

    public static String systemTime() {
        return "import \"system\"\n"
                + "import \"array\"\n"
                + "array.from(rows: [{time: system.time()}])";
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

    private static String measurementGroup() {
        return " |> group(columns: [\"_measurement\"])";
    }

    private static String cursorFilter(MeasurementOrder order, MeasurementCursor cursor) {
        if (cursor == null) {
            return "";
        }
        String observedAt = "time(v: " + stringLiteral(cursor.observedAt().toString()) + ")";
        String connectorId = stringLiteral(cursor.submittedByConnectorId().value().toString());
        return switch (order) {
            case ASC -> " |> filter(fn: (r) => r._time > " + observedAt
                    + " or (r._time == " + observedAt + " and r.submittedByConnectorId > " + connectorId + "))";
            case DESC -> " |> filter(fn: (r) => r._time < " + observedAt
                    + " or (r._time == " + observedAt + " and r.submittedByConnectorId < " + connectorId + "))";
        };
    }

    private static String sortByMeasurementPosition(MeasurementOrder order) {
        return " |> sort(columns: [\"_time\", \"submittedByConnectorId\"], desc: " + (order == MeasurementOrder.DESC) + ")";
    }

    private static String aggregateWindow(FluxDuration bucket, String function) {
        requireNonNull(bucket);
        requireNotEmpty(function);
        return " |> aggregateWindow(every: " + bucket + ", fn: " + function + ", createEmpty: false, timeSrc: \"_start\")";
    }

    static String stringLiteral(String value) {
        requireNotEmpty(value);
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
