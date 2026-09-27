package at.pegelhub.measurement.application;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class StepIntervalCalculator {
    private StepIntervalCalculator() { }

    static List<MeasurementInterval> calculate(
            Instant from, Duration width, int count, Instant computedAt,
            List<MeasurementReadRow> evidence) {
        requireNonNull(from);
        requireNonNull(width);
        requireNonNull(computedAt);
        Instant selectedTo = from.plus(width.multipliedBy(count));
        requireNonNull(evidence);
        Sample active = latestPredecessor(evidence, from);
        List<Sample> samples = uniqueSamples(evidence, from, selectedTo);
        List<MeasurementInterval> result = new ArrayList<>(count);
        int sampleIndex = 0;
        long intervalNanos = width.toNanos();

        for (int window = 0; window < count; window++) {
            Instant start = from.plus(width.multipliedBy(window));
            Instant end = start.plus(width);
            // Open windows expose only elapsed support, never support from future time.
            Instant evaluatedUntil = computedAt.isBefore(end) ? computedAt : end;
            Instant cursor = start;
            long supportedNanos = 0;
            double weightedMean = 0;
            long observationCount = 0;
            Instant lastContributing = null;

            while (cursor.isBefore(evaluatedUntil)) {
                if (sampleIndex < samples.size() && samples.get(sampleIndex).observedAt().equals(cursor)) {
                    active = samples.get(sampleIndex++);
                    observationCount++;
                    continue;
                }
                Instant nextObservation = sampleIndex < samples.size()
                        && samples.get(sampleIndex).observedAt().isBefore(evaluatedUntil)
                        ? samples.get(sampleIndex).observedAt() : evaluatedUntil;
                if (active != null) {
                    long nanos = Duration.between(cursor, nextObservation).toNanos();
                    supportedNanos += nanos;
                    // Normalize first: value * nanoseconds can overflow even when the mean is finite.
                    weightedMean = Math.fma(active.value(), (double) nanos / intervalNanos, weightedMean);
                    lastContributing = active.observedAt();
                }
                cursor = nextObservation;
            }

            String supportStatus = supportedNanos == intervalNanos ? "full"
                    : supportedNanos == 0 ? "absent" : "partial";
            Double mean = supportedNanos == intervalNanos ? weightedMean : null;
            if (mean != null && !Double.isFinite(mean)) {
                throw new IllegalStateException("Interval mean is non-finite");
            }
            result.add(new MeasurementInterval(start, end, mean, observationCount,
                    supportedNanos, lastContributing,
                    end.isAfter(computedAt) ? "open" : "closed", supportStatus));
        }
        return result;
    }

    private static Sample latestPredecessor(List<MeasurementReadRow> candidates, Instant from) {
        Sample latest = null;
        for (MeasurementReadRow row : candidates) {
            if (row.observedAt().isBefore(from)
                    && (latest == null || row.observedAt().isAfter(latest.observedAt()))) {
                latest = new Sample(row.observedAt(), row.value());
            }
        }
        if (latest == null) return null;

        // Only the newest predecessor contributes. Validate its ties after selection so
        // superseded writer conflicts cannot make the result depend on storage row order.
        for (MeasurementReadRow row : candidates) {
            if (row.observedAt().equals(latest.observedAt())) {
                requireSameValue(latest, row);
            }
        }
        return latest;
    }

    private static List<Sample> uniqueSamples(List<MeasurementReadRow> rows, Instant from, Instant to) {
        List<MeasurementReadRow> ordered = rows.stream()
                .filter(row -> !row.observedAt().isBefore(from))
                .sorted(Comparator.comparing(MeasurementReadRow::observedAt))
                .toList();
        List<Sample> result = new ArrayList<>(ordered.size());
        for (MeasurementReadRow row : ordered) {
            if (!row.observedAt().isBefore(to)) {
                throw new IllegalStateException("Storage returned an observation outside the requested window");
            }
            if (!result.isEmpty() && result.getLast().observedAt().equals(row.observedAt())) {
                requireSameValue(result.getLast(), row);
            } else {
                result.add(new Sample(row.observedAt(), row.value()));
            }
        }
        return result;
    }

    private static void requireSameValue(Sample existing, MeasurementReadRow candidate) {
        if (existing.value() != candidate.value()) {
            throw new IllegalStateException("Conflicting observations at " + candidate.observedAt());
        }
    }

    private record Sample(Instant observedAt, double value) { }
}
