package at.pegelhub.lib.config;

import java.time.Duration;

/** Polling settings for connectors that replay recent measurement windows. */
public record WindowedPollingConfig(String interval, String overlap) {
    public static final Duration DEFAULT_OVERLAP = Duration.ofHours(1);

    public Duration duration() {
        return new PollingConfig(interval).duration();
    }

    public Duration overlapDuration() {
        if (overlap == null) {
            return DEFAULT_OVERLAP;
        }
        try {
            return new PollingConfig(overlap).duration();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "polling.overlap must use a positive number followed by s, m, or h", e);
        }
    }
}
