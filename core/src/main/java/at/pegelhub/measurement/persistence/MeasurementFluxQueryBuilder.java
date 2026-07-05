package at.pegelhub.measurement.persistence;

import at.pegelhub.measurement.application.MeasurementBucketQuery;
import at.pegelhub.measurement.application.MeasurementCursor;
import at.pegelhub.measurement.application.MeasurementListQuery;
import at.pegelhub.measurement.application.MeasurementOrder;
import at.pegelhub.shared.duration.PegelhubDurationLiteral;
import at.pegelhub.shared.influx.DatabaseProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

import static at.pegelhub.shared.validation.Validations.requireNotEmpty;
import static java.util.Objects.requireNonNull;

@Component
final class MeasurementFluxQueryBuilder {

    private final DatabaseProperties database;

    MeasurementFluxQueryBuilder(@Qualifier("dataConfiguration") DatabaseProperties database) {
        this.database = requireNonNull(database);
    }

    String page(MeasurementListQuery query, int fetchLimit) {
        requireNonNull(query);
        if (fetchLimit < 1) {
            throw new IllegalArgumentException("fetchLimit must be positive");
        }
        String measurementRows = measurementWindow(
                query.timeSeriesId().value(),
                query.window().from(),
                query.window().to())
                + cursorFilter(query.order(), query.cursor());
        String pageOperations = " |> group(columns: [])"
                + sortByMeasurementPosition(query.order())
                + " |> limit(n: " + fetchLimit + ")";

        return measurementRows
                + valueFieldFilter()
                + pageOperations
                + " |> rename(columns: {_value: \"value\"})"
                + " |> keep(columns: [\"_time\", \"submittedByConnectorId\", \"value\"])";
    }

    String meanBuckets(MeasurementBucketQuery query) {
        return bucketQuery(query, "mean");
    }

    String countBuckets(MeasurementBucketQuery query) {
        return bucketQuery(query, "count");
    }

    String systemTime() {
        return "import \"system\"\n"
                + "import \"array\"\n"
                + "array.from(rows: [{time: system.time()}])";
    }

    private String bucketQuery(MeasurementBucketQuery query, String function) {
        requireNonNull(query);
        PegelhubDurationLiteral bucket = PegelhubDurationLiteral.from(query.resolution().bucketWidth().duration());
        return measurementWindow(
                query.timeSeriesId().value(),
                query.window().from(),
                query.window().to())
                + valueFieldFilter()
                + measurementGroup()
                + aggregateWindow(bucket, function);
    }

    private String measurementWindow(UUID measurement, Instant from, Instant to) {
        requireNonNull(from);
        requireNonNull(to);
        if (!to.isAfter(from)) {
            throw new IllegalArgumentException("to must be after from");
        }
        return from()
                + " |> range(start: time(v: " + stringLiteral(from.toString()) + "), stop: time(v: " + stringLiteral(to.toString()) + "))"
                + measurementFilter(measurement);
    }

    private String from() {
        return "from(bucket: " + stringLiteral(database.bucket()) + ")";
    }

    private String measurementFilter(UUID measurement) {
        requireNonNull(measurement);
        return " |> filter(fn: (r) => r._measurement == " + stringLiteral(measurement.toString()) + ")";
    }

    private String valueFieldFilter() {
        return " |> filter(fn: (r) => r._field == \"value\")";
    }

    private String measurementGroup() {
        return " |> group(columns: [\"_measurement\"])";
    }

    private String cursorFilter(MeasurementOrder order, MeasurementCursor cursor) {
        requireNonNull(order);
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

    private String sortByMeasurementPosition(MeasurementOrder order) {
        requireNonNull(order);
        return " |> sort(columns: [\"_time\", \"submittedByConnectorId\"], desc: " + (order == MeasurementOrder.DESC) + ")";
    }

    private String aggregateWindow(PegelhubDurationLiteral bucket, String function) {
        requireNonNull(bucket);
        requireNotEmpty(function);
        return " |> aggregateWindow(every: " + bucket + ", fn: " + function + ", createEmpty: false, timeSrc: \"_start\")";
    }

    private String stringLiteral(String value) {
        requireNotEmpty(value);
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
