package at.pegelhub.watchdog.monitoring;

import java.time.Instant;

/** UNKNOWN is a failed observation, never a healthy route or an acknowledgement of an earlier fault. */
public record CheckState(
        Status status,
        String reason,
        Instant evaluatedAt,
        Instant evidenceAt,
        Double ageSeconds) {

    public enum Status {
        OK,
        CRITICAL,
        UNKNOWN
    }

    public static CheckState unknown(String reason, Instant now) {
        return new CheckState(Status.UNKNOWN, reason, now, null, null);
    }

    /** The receiver uses two event tokens; the reason distinguishes stale data from an unavailable check. */
    public String signal() {
        return status == Status.OK ? "OK" : "ERR";
    }
}
