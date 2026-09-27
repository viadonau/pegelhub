package at.pegelhub.measurement.persistence;

import at.pegelhub.measurement.application.MeasurementListQuery;
import at.pegelhub.measurement.application.MeasurementLatestQuery;
import at.pegelhub.measurement.application.MeasurementOrder;
import at.pegelhub.measurement.application.MeasurementWindow;
import at.pegelhub.shared.influx.DatabaseProperties;
import at.pegelhub.timeseries.domain.TimeSeriesId;
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

    String intervalObservations(TimeSeriesId timeSeriesId, MeasurementWindow window) {
        return measurementWindow(timeSeriesId.value(), window.from(), window.to())
                + valueFieldFilter()
                + " |> rename(columns: {_value: \"value\"})"
                + " |> keep(columns: [\"_time\", \"submittedByConnectorId\", \"value\"])";
    }

    String intervalPredecessors(TimeSeriesId timeSeriesId, Instant before) {
        requireNonNull(timeSeriesId);
        requireNonNull(before);
        return from()
                + " |> range(start: 0, stop: time(v: " + stringLiteral(before.toString()) + "))"
                + measurementFilter(timeSeriesId.value())
                + valueFieldFilter()
                + " |> last()"
                + " |> rename(columns: {_value: \"value\"})"
                + " |> keep(columns: [\"_time\", \"submittedByConnectorId\", \"value\"])";
    }

    String latestMeasurements(MeasurementLatestQuery query) {
        requireNonNull(query);
        if (query.timeSeriesIds().isEmpty()) {
            throw new IllegalArgumentException("timeSeriesIds must not be empty");
        }
        String measurementPredicate = query.timeSeriesIds().stream()
                .map(id -> "r._measurement == " + stringLiteral(id.value().toString()))
                .collect(Collectors.joining(" or "));
        // Static predicates and last() run in storage, before merging connector-tagged tables.
        // Sort only their latest candidates to preserve the timestamp/connector tie-break.
        return from()
                + " |> range(start: time(v: " + stringLiteral(query.window().from().toString())
                + "), stop: time(v: " + stringLiteral(query.window().to().toString()) + "))"
                + " |> filter(fn: (r) => r._field == \"value\")"
                + " |> filter(fn: (r) => " + measurementPredicate + ")"
                + " |> last()"
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

    private String sortByMeasurementPosition(MeasurementOrder order) {
        requireNonNull(order);
        return " |> sort(columns: [\"_time\", \"submittedByConnectorId\"], desc: " + (order == MeasurementOrder.DESC) + ")";
    }

    private String stringLiteral(String value) {
        requireNotEmpty(value);
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
