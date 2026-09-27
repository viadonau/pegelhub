package at.pegelhub.connector.tstp;

import at.pegelhub.lib.config.PollingConfig;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

/** Selects Core-computed interval means for an existing outbound mapping. */
public record TstpMeanExport(
        String interval,
        String timeBasis,
        String settlingDelay) {
    public TstpMeanExport {
        if (!("15m".equals(interval) || "1h".equals(interval) || "1d".equals(interval))) {
            throw new IllegalArgumentException("meanExport.interval must be 15m, 1h, or 1d");
        }
        if (!("UTC".equals(timeBasis) || "+01:00".equals(timeBasis))) {
            throw new IllegalArgumentException("meanExport.timeBasis must be UTC or +01:00");
        }
        if (settlingDelay == null || !settlingDelay.matches("[1-9][0-9]*[smh]")) {
            throw new IllegalArgumentException("meanExport.settlingDelay must be a positive s, m, or h duration");
        }
        new PollingConfig(settlingDelay).duration();
    }

    public Duration width() {
        return switch (interval) {
            case "15m" -> Duration.ofMinutes(15);
            case "1h" -> Duration.ofHours(1);
            case "1d" -> Duration.ofDays(1);
            default -> throw new IllegalStateException("Unsupported mean export interval: " + interval);
        };
    }

    public Duration settlingDuration() {
        return new PollingConfig(settlingDelay).duration();
    }

    Instant alignedFloor(Instant instant) {
        long widthSeconds = width().getSeconds();
        long offsetSeconds = timeOffset().getTotalSeconds();
        long alignedSecond = Math.floorDiv(instant.getEpochSecond() + offsetSeconds, widthSeconds)
                * widthSeconds - offsetSeconds;
        return Instant.ofEpochSecond(alignedSecond);
    }

    private ZoneOffset timeOffset() {
        return "UTC".equals(timeBasis) ? ZoneOffset.UTC : ZoneOffset.of(timeBasis);
    }
}
