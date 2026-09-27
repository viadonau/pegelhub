package at.pegelhub.lib.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A complete Core-computed time-weighted interval response; no connector-side aggregation is needed. */
public record MeasurementIntervalStatistics(
        UUID timeSeriesId,
        Instant from,
        Instant to,
        String interval,
        String timeBasis,
        boolean closedOnly,
        String representation,
        String unit,
        String method,
        Instant computedAt,
        List<Interval> intervals) {

    public record Interval(
            Instant from,
            Instant to,
            Double mean,
            long observationCount,
            long supportedNanos,
            Instant lastContributingObservedAt,
            String windowStatus,
            String supportStatus) {
    }

    public void requireMatches(UUID expectedId, Instant expectedFrom, Instant expectedTo,
                               String expectedInterval, String expectedTimeBasis,
                               boolean expectedClosedOnly, MeasurementRepresentation expectedRepresentation) {
        expectedRepresentation.requireResponse(representation, unit);
        requireMetadataMatches(expectedId, expectedFrom, expectedTo, expectedInterval,
                expectedTimeBasis, expectedClosedOnly);
        long width = widthSeconds();
        long count = requireWindowCount(width);
        if (intervals.size() != selectedWindowCount(count, width)) {
            throw new IllegalStateException("Core returned incomplete interval results");
        }
        for (int index = 0; index < intervals.size(); index++) {
            Instant start = from.plusSeconds((long) index * width);
            requireInterval(intervals.get(index), start, start.plusSeconds(width), width);
        }
    }

    private void requireMetadataMatches(UUID expectedId, Instant expectedFrom, Instant expectedTo,
                                        String expectedInterval, String expectedTimeBasis, boolean expectedClosedOnly) {
        if (!expectedId.equals(timeSeriesId) || !expectedFrom.equals(from) || !expectedTo.equals(to)
                || !expectedInterval.equals(interval) || !expectedTimeBasis.equals(timeBasis)
                || closedOnly != expectedClosedOnly || !"time-weighted-step".equals(method)
                || computedAt == null || intervals == null) {
            throw new IllegalStateException("Core returned mismatched or incomplete interval metadata");
        }
    }

    private long widthSeconds() {
        return switch (interval) {
            case "15m" -> 900;
            case "1h" -> 3_600;
            case "1d" -> 86_400;
            default -> throw new IllegalStateException("Core returned an invalid interval width");
        };
    }

    private long requireWindowCount(long width) {
        long count = (to.getEpochSecond() - from.getEpochSecond()) / width;
        long offset = "+01:00".equals(timeBasis) ? 3_600 : 0;
        if (count < 1 || count > 50_000 || from.getNano() != 0 || to.getNano() != 0
                || Math.floorMod(from.getEpochSecond() + offset, width) != 0
                || Math.floorMod(to.getEpochSecond() + offset, width) != 0
                || !from.plusSeconds(count * width).equals(to)) {
            throw new IllegalStateException("Core returned invalid interval boundaries");
        }
        return count;
    }

    private long selectedWindowCount(long count, long width) {
        if (!closedOnly) return count;
        long selected = 0;
        while (selected < count && !from.plusSeconds((selected + 1) * width).isAfter(computedAt)) {
            selected++;
        }
        return selected;
    }

    private void requireInterval(Interval point, Instant start, Instant end, long width) {
        String windowStatus = end.isAfter(computedAt) ? "open" : "closed";
        if (point == null || !start.equals(point.from()) || !end.equals(point.to())
                || !windowStatus.equals(point.windowStatus())) {
            throw new IllegalStateException("Core returned an invalid interval window");
        }
        requireSupport(point, width * 1_000_000_000L);
    }

    private static void requireSupport(Interval point, long widthNanos) {
        long supported = point.supportedNanos();
        String expectedSupport = supported == widthNanos ? "full" : supported == 0 ? "absent" : "partial";
        // A held predecessor can support a full interval with zero new observations.
        if (point.observationCount() < 0 || supported < 0 || supported > widthNanos
                || !expectedSupport.equals(point.supportStatus())) {
            throw new IllegalStateException("Core returned inconsistent interval support");
        }
        Double mean = point.mean();
        if ((supported == widthNanos) != (mean != null) || (mean != null && !Double.isFinite(mean))) {
            throw new IllegalStateException("Core returned inconsistent interval mean");
        }
        Instant lastContributing = point.lastContributingObservedAt();
        if ((supported > 0) != (lastContributing != null)
                || (lastContributing != null && !lastContributing.isBefore(point.to()))) {
            throw new IllegalStateException("Core returned inconsistent contributing observation timestamp");
        }
    }
}
