package at.pegelhub.measurement.application;

import at.pegelhub.measurement.domain.InternalProducerId;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.measurement.persistence.InternalProducerRepository;
import at.pegelhub.measurement.persistence.MeasurementRepository;
import at.pegelhub.measuringpoint.application.MeasuringPointService;
import at.pegelhub.shared.error.MetadataConflictException;
import at.pegelhub.shared.metadata.MetadataStatus;
import at.pegelhub.station.application.StationService;
import at.pegelhub.timeseries.application.TimeSeriesService;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Data-hub interface for in-process producers, not a substitute connector identity.
 * A producer can read its configured inputs and write only its exclusively assigned output.
 * Configuration is transactional metadata; measurement reads and writes use Influx separately.
 */
@Service
public class InternalMeasurements {

    private final InternalProducerRepository producers;
    private final MeasurementRepository measurements;
    private final TimeSeriesService series;
    private final MeasuringPointService points;
    private final StationService stations;
    private final Clock clock;

    public InternalMeasurements(
            InternalProducerRepository producers,
            MeasurementRepository measurements,
            TimeSeriesService series,
            MeasuringPointService points,
            StationService stations,
            Clock clock) {
        this.producers = producers;
        this.measurements = measurements;
        this.series = series;
        this.points = points;
        this.stations = stations;
        this.clock = clock;
    }

    /**
     * Atomically replaces bindings after validating active metadata, compatible properties,
     * exclusive ownership and an acyclic dependency graph. A null output makes the producer read-only.
     */
    @Transactional
    public void configure(InternalProducerId producer, List<TimeSeriesId> inputs, TimeSeriesId output) {
        if (inputs.isEmpty() || new HashSet<>(inputs).size() != inputs.size()) {
            throw new IllegalArgumentException("Sources must be nonempty and distinct");
        }

        // Serialize graph edits so concurrent, individually valid changes cannot jointly create a cycle.
        producers.lockConfiguration();
        validateBindings(inputs, output);

        producers.register(producer);
        var previous = producers.output(producer);
        if (previous != null && !previous.equals(output)) {
            series.assignInternalProducer(previous, producer, false);
        }
        if (output != null) {
            series.assignInternalProducer(output, producer, true);
        }

        producers.configure(producer, inputs, output);
    }

    private void validateBindings(List<TimeSeriesId> inputs, TimeSeriesId output) {
        String property = active(inputs.getFirst()).observedProperty().value();

        for (var input : inputs) {
            if (!active(input).observedProperty().value().equals(property)) {
                throw new IllegalArgumentException("Sources must share an observed property and canonical unit");
            }
            if (output != null && dependsOn(input, output, new HashSet<>())) {
                throw new MetadataConflictException("Derived output would create a dependency cycle");
            }
        }

        if (output != null && !active(output).observedProperty().value().equals(property)) {
            throw new IllegalArgumentException("Output must share the source property and canonical unit");
        }
    }

    /**
     * Returns complete rows for each configured input within the half-open window.
     * Values stay in canonical storage units, regardless of the inputs' connector representations.
     * Point and time budgets cover all inputs together; partial results are never returned.
     * An empty input remains empty for the caller to classify as missing data.
     *
     * @throws MeasurementReadLimitException if completeness cannot be established within the budgets
     */
    public Map<TimeSeriesId, List<MeasurementReadRow>> readWindow(
            InternalProducerId producer,
            MeasurementWindow window,
            int maximumPoints,
            Duration timeout) {
        if (maximumPoints < 1 || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Invalid run limits");
        }

        var inputs = producers.inputs(producer);
        if (inputs.isEmpty()) {
            throw new MetadataConflictException("Producer has no configured inputs");
        }

        long deadline = System.nanoTime() + timeout.toNanos();
        Map<TimeSeriesId, List<MeasurementReadRow>> result = new LinkedHashMap<>();
        int remaining = maximumPoints;

        for (var input : inputs) {
            active(input);

            var rows = new ArrayList<MeasurementReadRow>();
            readComplete(input, window, remaining, deadline, rows);

            remaining -= rows.size();
            result.put(input, List.copyOf(rows));
        }

        return Map.copyOf(result);
    }

    /**
     * Rechecks output ownership and writes at the observation time, not the run time.
     * Repeating the same producer/series/timestamp replaces that Influx point; a transport failure
     * can leave acceptance uncertain and cannot roll back PostgreSQL run history.
     */
    public void writeOutput(
            InternalProducerId producer,
            TimeSeriesId output,
            Instant observedAt,
            double value,
            Duration timeout) {
        if (!output.equals(producers.output(producer))) {
            throw new MetadataConflictException("Output is not assigned to producer");
        }

        var destination = active(output);
        if (destination.sourceAssignment() == null
                || !producer.equals(destination.sourceAssignment().internalProducerId())) {
            throw new MetadataConflictException("Output source assignment changed");
        }

        measurements.storeMeasurements(
                List.of(new Measurement(output, observedAt, clock.instant(), value, null, producer)), timeout);
    }

    private void readComplete(
            TimeSeriesId input,
            MeasurementWindow window,
            int budget,
            long deadline,
            List<MeasurementReadRow> rows) {
        if (Thread.currentThread().isInterrupted() || System.nanoTime() >= deadline) {
            throw new MeasurementReadLimitException(MeasurementReadLimitException.Code.DEADLINE);
        }

        int available = budget - rows.size();
        if (available < 1) {
            throw new MeasurementReadLimitException(MeasurementReadLimitException.Code.POINT_LIMIT);
        }

        var remaining = Duration.ofNanos(deadline - System.nanoTime());
        if (remaining.isNegative() || remaining.isZero()) {
            throw new MeasurementReadLimitException(MeasurementReadLimitException.Code.DEADLINE);
        }

        var page = measurements.listMeasurements(
                new MeasurementListQuery(input, window, MeasurementOrder.ASC, Math.min(10_000, available)), remaining);
        if (System.nanoTime() >= deadline) {
            throw new MeasurementReadLimitException(MeasurementReadLimitException.Code.DEADLINE);
        }

        if (!page.truncated()) {
            rows.addAll(page.measurements());
            return;
        }

        // Discard the truncated page and bisect [from, to): the halves neither overlap nor leave a gap.
        // At millisecond precision an unsplittable truncated interval must fail, never look complete.
        var midpoint = window.from().plusMillis(Duration.between(window.from(), window.to()).toMillis() / 2);
        if (!midpoint.isAfter(window.from())) {
            throw new MeasurementReadLimitException(MeasurementReadLimitException.Code.INCOMPLETE_WINDOW);
        }

        readComplete(input, new MeasurementWindow(window.from(), midpoint, null), budget, deadline, rows);
        readComplete(input, new MeasurementWindow(midpoint, window.to(), null), budget, deadline, rows);
    }

    private boolean dependsOn(TimeSeriesId source, TimeSeriesId output, Set<TimeSeriesId> visited) {
        if (source.equals(output)) {
            return true;
        }
        if (!visited.add(source)) {
            return false;
        }

        var assignment = series.get(source).sourceAssignment();
        if (assignment == null || assignment.internalProducerId() == null) {
            return false;
        }

        return producers.inputs(assignment.internalProducerId()).stream()
                .anyMatch(input -> dependsOn(input, output, visited));
    }

    private TimeSeries active(TimeSeriesId id) {
        var item = series.get(id);
        var point = points.get(item.measuringPointId());

        if (item.status() != MetadataStatus.ACTIVE || point.status() != MetadataStatus.ACTIVE
                || stations.get(point.stationId()).status() != MetadataStatus.ACTIVE) {
            throw new MetadataConflictException("Time series and its hierarchy must be active");
        }

        return item;
    }
}
