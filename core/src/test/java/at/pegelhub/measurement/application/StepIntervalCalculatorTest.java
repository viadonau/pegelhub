package at.pegelhub.measurement.application;

import at.pegelhub.connector.domain.ConnectorId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

final class StepIntervalCalculatorTest {
    private static final Instant START = Instant.parse("2026-09-23T10:00:00Z");
    private static final ConnectorId A = new ConnectorId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final ConnectorId B = new ConnectorId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
    private static final ConnectorId C = new ConnectorId(UUID.fromString("00000000-0000-0000-0000-000000000003"));

    @Test
    void temperatureHoldsAcrossTwentySixHoursWithoutRetrospectiveRamp() {
        var seed = row(START.minusSeconds(60), 18, A);
        var first = calculate(Duration.ofMinutes(15), 104, List.of(seed), List.of());
        var later = calculate(Duration.ofMinutes(15), 105, List.of(seed),
                List.of(row(START.plus(Duration.ofHours(26)), 17, A)));

        assertThat(first).containsExactlyElementsOf(later.subList(0, 104));
        assertThat(later.getLast().mean()).isEqualTo(17);
        assertThat(first).allSatisfy(interval -> {
            assertThat(interval.mean()).isEqualTo(18);
            assertThat(interval.observationCount()).isZero();
            assertThat(interval.supportedNanos()).isEqualTo(Duration.ofMinutes(15).toNanos());
            assertThat(interval.lastContributingObservedAt()).isEqualTo(seed.observedAt());
        });
    }

    @Test
    void valueAtEndStartsNextWindowAndEqualValueRefreshesAge() {
        var intervals = calculate(Duration.ofMinutes(15), 2,
                List.of(row(START.minusSeconds(1), 18, A)),
                List.of(row(START.plusSeconds(600), 18, A), row(START.plusSeconds(900), 17, A)));

        assertThat(intervals.get(0).mean()).isEqualTo(18);
        assertThat(intervals.get(0).observationCount()).isEqualTo(1);
        assertThat(intervals.get(0).lastContributingObservedAt()).isEqualTo(START.plusSeconds(600));
        assertThat(intervals.get(1).mean()).isEqualTo(17);
        assertThat(intervals.get(1).observationCount()).isEqualTo(1);
    }

    @Test
    void unevenSubsecondDurationsAreTimeWeightedNotRowWeighted() {
        var intervals = calculate(Duration.ofMinutes(15), 1,
                List.of(row(START.minusSeconds(1), 10, A)),
                List.of(row(START.plusMillis(450_500), 20, A)));

        assertThat(intervals.getFirst().mean()).isCloseTo(
                (10 * 450.5 + 20 * 449.5) / 900, offset(1e-12));
        assertThat(intervals.getFirst().supportedNanos()).isEqualTo(Duration.ofMinutes(15).toNanos());
    }

    @Test
    void requiresPredecessorForBeginningOfFirstInterval() {
        var intervals = calculate(Duration.ofMinutes(15), 2, List.of(),
                List.of(row(START.plusSeconds(100), 12, A)));

        assertThat(intervals.get(0).mean()).isNull();
        assertThat(intervals.get(0).supportStatus()).isEqualTo("partial");
        assertThat(intervals.get(0).supportedNanos()).isEqualTo(Duration.ofSeconds(800).toNanos());
        assertThat(intervals.get(1).mean()).isEqualTo(12);
        assertThat(intervals.get(1).observationCount()).isZero();
    }

    @Test
    void hourlyAndDailyMeansUseSameDirectIntegration() {
        var seed = row(START.minusSeconds(1), 10, A);
        var noon = row(START.plus(Duration.ofHours(12)), 20, A);
        var daily = calculate(Duration.ofDays(1), 1, List.of(seed), List.of(noon));
        var hourly = calculate(Duration.ofHours(1), 24, List.of(seed), List.of(noon));

        assertThat(daily.getFirst().mean()).isEqualTo(15);
        assertThat(hourly.get(0).mean()).isEqualTo(10);
        assertThat(hourly.get(12).mean()).isEqualTo(20);
    }

    @Test
    void identicalSimultaneousReadingsCollapseAndConflictsFail() {
        var seed = row(START.minusSeconds(1), 10, A);
        var sameAtBothWriters = List.of(row(START, 12, A), row(START, 12, B));
        assertThat(calculate(Duration.ofMinutes(15), 1, List.of(seed), sameAtBothWriters)
                .getFirst().observationCount()).isEqualTo(1);

        assertThatThrownBy(() -> calculate(Duration.ofMinutes(15), 1, List.of(seed),
                List.of(row(START, 12, A), row(START, 13, B))))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Conflicting");
        assertThatThrownBy(() -> calculate(Duration.ofMinutes(15), 1,
                List.of(row(START.minusSeconds(1), 12, A), row(START.minusSeconds(1), 13, B)), List.of()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Conflicting");
    }

    @Test
    void supersededPredecessorConflictsDoNotDependOnWriterOrder() {
        var oldA = row(START.minusSeconds(600), 18, A);
        var oldB = row(START.minusSeconds(600), 19, B);
        var newest = row(START.minusSeconds(60), 20, C);

        for (var predecessors : permutations(oldA, oldB, newest)) {
            var interval = calculate(Duration.ofMinutes(15), 1, predecessors, List.of()).getFirst();

            assertThat(interval.mean()).isEqualTo(20);
            assertThat(interval.supportStatus()).isEqualTo("full");
            assertThat(interval.observationCount()).isZero();
            assertThat(interval.lastContributingObservedAt()).isEqualTo(newest.observedAt());
        }
    }

    @Test
    void latestPredecessorConflictsFailInEveryWriterOrder() {
        var older = row(START.minusSeconds(600), 18, C);
        var newestA = row(START.minusSeconds(60), 19, A);
        var newestB = row(START.minusSeconds(60), 20, B);

        for (var predecessors : permutations(older, newestA, newestB)) {
            assertThatThrownBy(() -> calculate(Duration.ofMinutes(15), 1, predecessors, List.of()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Conflicting observations at " + newestA.observedAt());
        }
    }

    @Test
    void openAndFutureWindowsNeverClaimUnevaluatedTimeAsSupported() {
        Instant computedAt = START.plusSeconds(300);
        var intervals = StepIntervalCalculator.calculate(
                START,
                Duration.ofMinutes(15),
                2,
                computedAt,
                List.of(
                        row(START.minusSeconds(1), 10, A),
                        row(START.plusSeconds(120), 20, A),
                        row(START.plusSeconds(600), 30, A)));

        assertThat(intervals.get(0)).satisfies(interval -> {
            assertThat(interval.mean()).isNull();
            assertThat(interval.supportStatus()).isEqualTo("partial");
            assertThat(interval.supportedNanos()).isEqualTo(Duration.ofMinutes(5).toNanos());
            assertThat(interval.observationCount()).isEqualTo(1);
            assertThat(interval.lastContributingObservedAt()).isEqualTo(START.plusSeconds(120));
            assertThat(interval.windowStatus()).isEqualTo("open");
        });
        assertThat(intervals.get(1)).satisfies(interval -> {
            assertThat(interval.mean()).isNull();
            assertThat(interval.supportStatus()).isEqualTo("absent");
            assertThat(interval.supportedNanos()).isZero();
            assertThat(interval.observationCount()).isZero();
            assertThat(interval.lastContributingObservedAt()).isNull();
            assertThat(interval.windowStatus()).isEqualTo("open");
        });
    }

    private List<MeasurementInterval> calculate(Duration width, int count,
                                                List<MeasurementReadRow> predecessors,
                                                List<MeasurementReadRow> observations) {
        return StepIntervalCalculator.calculate(START, width, count, START.plus(width.multipliedBy(count)),
                Stream.concat(predecessors.stream(), observations.stream()).toList());
    }

    private static MeasurementReadRow row(Instant at, double value, ConnectorId writer) {
        return new MeasurementReadRow(at, value, writer);
    }

    private static List<List<MeasurementReadRow>> permutations(
            MeasurementReadRow a, MeasurementReadRow b, MeasurementReadRow c) {
        return List.of(List.of(a, b, c), List.of(a, c, b), List.of(b, a, c),
                List.of(b, c, a), List.of(c, a, b), List.of(c, b, a));
    }
}
