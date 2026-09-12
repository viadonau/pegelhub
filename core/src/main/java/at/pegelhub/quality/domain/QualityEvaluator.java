package at.pegelhub.quality.domain;

import at.pegelhub.measurement.application.MeasurementReadRow;
import at.pegelhub.timeseries.domain.TimeSeriesId;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Advisory rule calculations over a supplied window, independent of persistence and transports.
 * Missing sources produce DATA findings and suppress output. The caller must reject failed or
 * truncated reads before evaluation: a list of rows cannot prove that a read was complete.
 */
public final class QualityEvaluator {

    private static final double FROZEN_VALUE_TOLERANCE = 0.001;

    private QualityEvaluator() { }

    public record Output(Instant observedAt, double value) { }

    /** A complete result can still have findings, or no output because latest timestamps differ. */
    public record Result(List<Finding> findings, Output output, boolean complete) {
        public Result {
            findings = List.copyOf(findings);
        }
    }

    private record Sample(UUID series, double value) { }

    /**
     * Evaluates only configured sources without changing their rows. Output requires every source,
     * no findings (including warnings), and exact alignment of the latest observation timestamps.
     * No timestamp interpolation or substitution of an earlier aligned set is performed.
     */
    public static Result evaluate(ProfileConfig profile, Map<TimeSeriesId, List<MeasurementReadRow>> window) {
        var findings = new ArrayList<Finding>();
        var latest = new ArrayList<MeasurementReadRow>();
        var peers = new TreeMap<Instant, List<Sample>>();
        boolean complete = true;

        for (var id : profile.sources()) {
            var rows = window.getOrDefault(new TimeSeriesId(id), List.of()).stream()
                    .sorted(Comparator.comparing(MeasurementReadRow::observedAt))
                    .toList();

            if (rows.isEmpty()) {
                complete = false;
                findings.add(new Finding(Finding.Rule.DATA, Finding.Severity.WARNING, List.of(id), null,
                        "Keine Messwerte im Zeitfenster", Map.of()));
                continue;
            }

            latest.add(rows.getLast());
            checkRangeAndJump(id, rows, profile.rules(), findings);
            checkFrozenValue(id, rows, profile.rules(), findings);
            if (profile.rules().maxDeviation() != null) {
                collectPeers(id, rows, peers);
            }
        }

        checkDeviation(peers, profile.rules(), findings);
        return new Result(findings, deriveOutput(profile, latest, complete, findings), complete);
    }

    private static void checkRangeAndJump(
            UUID id,
            List<MeasurementReadRow> rows,
            QualityRules rules,
            List<Finding> findings) {
        boolean jumpReported = false;

        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i);

            if (rules.range() != null && (row.value() < rules.range().min() || row.value() > rules.range().max())) {
                findings.add(new Finding(Finding.Rule.RANGE, Finding.Severity.FAILED, List.of(id), row.observedAt(),
                        "Messwert ausserhalb des Bereichs",
                        Map.of("value", row.value(), "min", rules.range().min(), "max", rules.range().max())));
            }

            // Range findings retain every violation; jump reports only the first violation per source.
            if (i > 0 && !jumpReported && rules.jumpThreshold() != null
                    && Math.abs(row.value() - rows.get(i - 1).value()) > rules.jumpThreshold()) {
                findings.add(new Finding(Finding.Rule.JUMP, Finding.Severity.FAILED, List.of(id), row.observedAt(),
                        "Sprung zwischen aufeinanderfolgenden Messwerten",
                        Map.of("previous", rows.get(i - 1).value(), "value", row.value())));
                jumpReported = true;
            }
        }
    }

    private static void checkFrozenValue(
            UUID id,
            List<MeasurementReadRow> rows,
            QualityRules rules,
            List<Finding> findings) {
        if (rules.frozenSeconds() == null) {
            return;
        }

        var last = rows.getLast();
        int start = rows.size() - 1;

        // Only the trailing stable sequence matters; an earlier plateau is not current frozen input.
        while (start > 0 && Math.abs(last.value() - rows.get(start - 1).value()) <= FROZEN_VALUE_TOLERANCE) {
            start--;
        }

        if (start < rows.size() - 1
                && Duration.between(rows.get(start).observedAt(), last.observedAt()).getSeconds() >= rules.frozenSeconds()) {
            findings.add(new Finding(Finding.Rule.FROZEN, Finding.Severity.WARNING, List.of(id), last.observedAt(),
                    "Messwert bleibt unveraendert", Map.of("value", last.value())));
        }
    }

    private static void collectPeers(UUID id, List<MeasurementReadRow> rows, Map<Instant, List<Sample>> peers) {
        for (var row : rows) {
            peers.computeIfAbsent(row.observedAt(), ignored -> new ArrayList<>()).add(new Sample(id, row.value()));
        }
    }

    private static void checkDeviation(Map<Instant, List<Sample>> peers, QualityRules rules, List<Finding> findings) {
        if (rules.maxDeviation() == null) {
            return;
        }

        for (var entry : peers.entrySet()) {
            var ids = entry.getValue().stream().map(Sample::series).distinct().sorted().toList();
            if (ids.size() < 2) {
                continue;
            }

            double min = entry.getValue().stream().mapToDouble(Sample::value).min().orElseThrow();
            double max = entry.getValue().stream().mapToDouble(Sample::value).max().orElseThrow();

            if (max - min > rules.maxDeviation()) {
                findings.add(new Finding(Finding.Rule.DEVIATION, Finding.Severity.FAILED, ids, entry.getKey(),
                        "Abweichung zwischen Zeitreihen", Map.of("min", min, "max", max)));
            }
        }
    }

    private static Output deriveOutput(
            ProfileConfig profile,
            List<MeasurementReadRow> latest,
            boolean complete,
            List<Finding> findings) {
        if (profile.output() == null || !complete || !findings.isEmpty()
                || latest.stream().map(MeasurementReadRow::observedAt).distinct().count() != 1) {
            return null;
        }

        // Summing finite doubles can overflow even when the mean is finite. Preserve that mean.
        double mean = latest.stream()
                .map(row -> BigDecimal.valueOf(row.value()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(latest.size()), MathContext.DECIMAL128)
                .doubleValue();

        return new Output(latest.getFirst().observedAt(), mean);
    }
}
