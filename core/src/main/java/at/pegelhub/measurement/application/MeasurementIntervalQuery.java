package at.pegelhub.measurement.application;

import at.pegelhub.timeseries.domain.MeasurementRepresentation;
import at.pegelhub.timeseries.domain.TimeSeriesId;

import java.time.Duration;
import java.time.Instant;

import static java.util.Objects.requireNonNull;

public record MeasurementIntervalQuery(
        TimeSeriesId timeSeriesId,
        Instant from,
        Instant to,
        String interval,
        String timeBasis,
        boolean closedOnly,
        MeasurementRepresentation representation) {

    private static final int MAX_WINDOWS = 50_000;
    public MeasurementIntervalQuery {
        requireNonNull(timeSeriesId);
        requireNonNull(from);
        requireNonNull(to);
        requireNonNull(representation);
        long seconds = intervalWidth(interval).getSeconds();
        long offset = timeOffsetSeconds(timeBasis);
        if (!to.isAfter(from) || from.getNano() != 0 || to.getNano() != 0
                || Math.floorMod(from.getEpochSecond() + offset, seconds) != 0
                || Math.floorMod(to.getEpochSecond() + offset, seconds) != 0) {
            throw new IllegalArgumentException("from and to must be aligned full-interval boundaries");
        }
        long count = Duration.between(from, to).getSeconds() / seconds;
        if (count > MAX_WINDOWS) {
            throw new IllegalArgumentException("interval query exceeds " + MAX_WINDOWS + " windows");
        }
    }

    public Duration width() {
        return intervalWidth(interval);
    }

    private static Duration intervalWidth(String interval) {
        if (interval == null) {
            throw new IllegalArgumentException("interval must be 15m, 1h or 1d");
        }
        return switch (interval) {
            case "15m" -> Duration.ofMinutes(15);
            case "1h" -> Duration.ofHours(1);
            case "1d" -> Duration.ofDays(1);
            default -> throw new IllegalArgumentException("interval must be 15m, 1h or 1d");
        };
    }

    private static long timeOffsetSeconds(String timeBasis) {
        if (timeBasis == null) {
            throw new IllegalArgumentException("timeBasis must be UTC or +01:00");
        }
        return switch (timeBasis) {
            case "UTC" -> 0;
            case "+01:00" -> 3_600;
            default -> throw new IllegalArgumentException("timeBasis must be UTC or +01:00");
        };
    }
}
