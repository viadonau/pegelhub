package at.pegelhub.watchdog.monitoring;

import java.time.Duration;
import java.time.Instant;

/** Receipt freshness, not value plausibility or proof that every source sample returned. */
public final class Checks {
    private Checks() {
    }

    public static CheckState evaluate(Instant latest, Instant now, Duration silence) {
        if (latest == null) {
            return new CheckState(CheckState.Status.CRITICAL, "no_measurement", now, null, null);
        }
        if (latest.isAfter(now)) {
            return new CheckState(CheckState.Status.UNKNOWN, "future_timestamp", now, latest, null);
        }

        Duration age = Duration.between(latest, now);
        // The limit is inclusive: a sample becomes stale only after the allowed age has elapsed.
        boolean stale = age.compareTo(silence) > 0;
        return new CheckState(
                stale ? CheckState.Status.CRITICAL : CheckState.Status.OK,
                stale ? "stale" : "fresh",
                now,
                latest,
                age.toMillis() / 1000.0);
    }
}
