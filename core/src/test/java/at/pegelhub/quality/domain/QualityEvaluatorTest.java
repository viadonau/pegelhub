package at.pegelhub.quality.domain;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measurement.application.MeasurementReadRow;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class QualityEvaluatorTest {

    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();
    private final UUID output = UUID.randomUUID();
    private final ConnectorId connector = new ConnectorId(UUID.randomUUID());
    private final Instant time = Instant.parse("2026-09-13T08:00:00Z");

    @Test
    void rangeIsInclusiveAndJumpReportsFirstFailurePerSeries() {
        var profile = profile(List.of(a), new QualityRules(new QualityRules.Range(0, 10), 5.0, null, null));

        var result = QualityEvaluator.evaluate(profile,
                Map.of(new TimeSeriesId(a), List.of(row(0, 0), row(60, 10), row(120, 11))));

        assertThat(result.findings()).extracting(Finding::rule).containsExactly(Finding.Rule.JUMP, Finding.Rule.RANGE);
        assertThat(result.output()).isNull();
    }

    @Test
    void frozenChecksOnlyTrailingRunWithTolerance() {
        var profile = profile(List.of(a), new QualityRules(null, null, 60L, null));

        var unchanged = QualityEvaluator.evaluate(profile,
                Map.of(new TimeSeriesId(a), List.of(row(0, 9), row(60, 1), row(120, 1.0005))));
        assertThat(unchanged.findings()).singleElement().satisfies(f -> assertThat(f.rule()).isEqualTo(Finding.Rule.FROZEN));

        var changed = QualityEvaluator.evaluate(profile,
                Map.of(new TimeSeriesId(a), List.of(row(0, 1), row(60, 1), row(120, 9))));
        assertThat(changed.findings()).isEmpty();
    }

    @Test
    void deviationRequiresDistinctPeersAtExactlyMatchingTimes() {
        var profile = profile(List.of(a, b), new QualityRules(null, null, null, 5.0));

        var result = QualityEvaluator.evaluate(profile, Map.of(
                new TimeSeriesId(a), List.of(row(0, 1), row(60, 1)),
                new TimeSeriesId(b), List.of(row(1, 100), row(60, 10))));

        assertThat(result.findings()).singleElement().satisfies(f -> {
            assertThat(f.rule()).isEqualTo(Finding.Rule.DEVIATION);
            assertThat(f.observedAt()).isEqualTo(time.plusSeconds(60));
        });
    }

    @Test
    void missingOrMisalignedInputsNeverProduceSyntheticOutput() {
        var profile = profile(List.of(a, b), null);

        var missing = QualityEvaluator.evaluate(profile, Map.of(new TimeSeriesId(a), List.of(row(0, 4))));
        assertThat(missing.complete()).isFalse();
        assertThat(missing.output()).isNull();
        assertThat(missing.findings()).extracting(Finding::rule).containsExactly(Finding.Rule.DATA);

        var misaligned = QualityEvaluator.evaluate(profile, Map.of(
                new TimeSeriesId(a), List.of(row(0, 4)),
                new TimeSeriesId(b), List.of(row(1, 8))));
        assertThat(misaligned.output()).isNull();
    }

    @Test
    void alignedAveragePreservesInstantAndHandlesLargeFiniteValues() {
        var profile = profile(List.of(a, b), null);

        var result = QualityEvaluator.evaluate(profile, Map.of(
                new TimeSeriesId(a), List.of(row(0, 4)),
                new TimeSeriesId(b), List.of(row(0, 8))));
        assertThat(result.output()).isEqualTo(new QualityEvaluator.Output(time, 6));

        var large = QualityEvaluator.evaluate(profile, Map.of(
                new TimeSeriesId(a), List.of(row(0, Double.MAX_VALUE)),
                new TimeSeriesId(b), List.of(row(0, Double.MAX_VALUE))));
        assertThat(large.output().value()).isFinite();
    }

    @Test
    void combinedRulesKeepFindingOrderAndLeaveInputOrderUnchanged() {
        var profile = profile(List.of(a, b), new QualityRules(new QualityRules.Range(0, 10), 5.0, 60L, 5.0));
        var unsorted = List.of(row(120, 20), row(0, 0), row(60, 20));

        var result = QualityEvaluator.evaluate(profile, Map.of(
                new TimeSeriesId(a), unsorted,
                new TimeSeriesId(b), List.of(row(60, 1), row(120, 2))));

        assertThat(result.complete()).isTrue();
        assertThat(result.findings()).extracting(Finding::rule).containsExactly(
                Finding.Rule.RANGE, Finding.Rule.JUMP, Finding.Rule.RANGE, Finding.Rule.FROZEN,
                Finding.Rule.DEVIATION, Finding.Rule.DEVIATION);
        assertThat(result.output()).isNull();
        assertThat(unsorted).extracting(MeasurementReadRow::observedAt)
                .containsExactly(time.plusSeconds(120), time, time.plusSeconds(60));
    }

    @Test
    void latestMeanDoesNotFallBackToAnEarlierAlignedSet() {
        var result = QualityEvaluator.evaluate(profile(List.of(a, b), null),
                Map.of(new TimeSeriesId(a), List.of(row(0, 4), row(60, 6)),
                        new TimeSeriesId(b), List.of(row(0, 8))));

        assertThat(result.complete()).isTrue();
        assertThat(result.findings()).isEmpty();
        assertThat(result.output()).isNull();
    }

    @Test
    void profilesRejectInvalidSettingsAndDefaultToRecentWindows() {
        assertThat(profile(List.of(a), null).lookbackSeconds()).isEqualTo(1200);
        assertThat(profile(List.of(a), null).intervalSeconds()).isEqualTo(60);

        assertThatThrownBy(() -> profile(List.of(a, a), null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new QualityRules(null, Double.NaN, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProfileConfig("name", false, List.of(a), null, 604801, 60, List.of(), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProfileConfig("name", false, List.of(a), null, 1200, 60, List.of(), a))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private ProfileConfig profile(List<UUID> sources, QualityRules rules) {
        return new ProfileConfig("Profile", true, sources, rules, null, null, List.of(), output);
    }

    private MeasurementReadRow row(long seconds, double value) {
        return new MeasurementReadRow(time.plusSeconds(seconds), value, connector);
    }
}
