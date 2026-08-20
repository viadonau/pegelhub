package at.pegelhub.measurement.persistence;

import at.pegelhub.measurement.application.MeasurementBucketQuery;
import at.pegelhub.measurement.application.MeasurementListQuery;
import at.pegelhub.measurement.application.MeasurementLatestQuery;
import at.pegelhub.measurement.application.MeasurementOrder;
import at.pegelhub.shared.duration.PegelhubDurationLiteral;
import at.pegelhub.shared.influx.DatabaseProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

import static at.pegelhub.shared.validation.Validations.requireNotEmpty;
import static java.util.Objects.requireNonNull;

@Component
final class MeasurementFluxQueryBuilder {

    private final DatabaseProperties database;

    MeasurementFluxQueryBuilder(@Qualifier("dataConfiguration") DatabaseProperties database) {
        this.database = requireNonNull(database);
    }

    String rawMeasurements(MeasurementListQuery query, int fetchLimit) {
        requireNonNull(query);
        if (fetchLimit < 1) {
            throw new IllegalArgumentException("fetchLimit must be positive");
        }
        String measurementRows = measurementWindow(
                query.timeSeriesId().value(),
                query.window().from(),
                query.window().to());
        String boundedReadOperations = " |> group(columns: [])"
                + sortByMeasurementPosition(query.order())
                + " |> limit(n: " + fetchLimit + ")";

        return measurementRows
                + valueFieldFilter()
                + boundedReadOperations
                + " |> rename(columns: {_value: \"value\"})"
                + " |> keep(columns: [\"_time\", \"submittedByConnectorId\", \"value\"])";
    }

    String meanBuckets(MeasurementBucketQuery query) {
        return bucketQuery(query, "mean");
    }

    String countBuckets(MeasurementBucketQuery query) {
        return bucketQuery(query, "count");
    }

    String latestMeasurements(MeasurementLatestQuery query) {
        requireNonNull(query);
        if (query.timeSeriesIds().isEmpty()) {
            throw new IllegalArgumentException("timeSeriesIds must not be empty");
        }
        String ids = query.timeSeriesIds().stream()
                .map(id -> stringLiteral(id.value().toString()))
                .collect(Collectors.joining(", "));
        return from()
                + " |> range(start: time(v: " + stringLiteral(query.window().from().toString())
                + "), stop: time(v: " + stringLiteral(query.window().to().toString()) + "))"
                + " |> filter(fn: (r) => r._field == \"value\")"
                + " |> filter(fn: (r) => contains(value: r._measurement, set: [" + ids + "]))"
                + " |> group(columns: [\"_measurement\"])"
                + " |> sort(columns: [\"_time\", \"submittedByConnectorId\"], desc: true)"
                + " |> limit(n: 1)"
                + " |> rename(columns: {_value: \"value\"})"
                + " |> keep(columns: [\"_measurement\", \"_time\", \"submittedByConnectorId\", \"value\"])";
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
