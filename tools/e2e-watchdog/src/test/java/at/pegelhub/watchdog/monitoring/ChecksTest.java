package at.pegelhub.watchdog.monitoring;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ChecksTest {
    private static final Instant NOON = Instant.parse("2026-09-16T12:00:00Z");
    private static final Duration TEN_MINUTES = Duration.ofMinutes(10);

    @Test
    void aSampleExpiresOnlyAfterTheConfiguredAge() {
        assertEquals(CheckState.Status.OK, Checks.evaluate(NOON, NOON.plusSeconds(600), TEN_MINUTES).status());

        var stale = Checks.evaluate(NOON, NOON.plusSeconds(601), TEN_MINUTES);
        assertEquals(CheckState.Status.CRITICAL, stale.status());
        assertEquals("stale", stale.reason());
        assertEquals(601.0, stale.ageSeconds());
    }

    @Test
    void noMeasurementsAndUntrustworthyTimestampsAreDistinctErrors() {
        assertEquals("no_measurement", Checks.evaluate(null, NOON, TEN_MINUTES).reason());

        var future = Checks.evaluate(NOON.plusSeconds(1), NOON, TEN_MINUTES);
        assertEquals(CheckState.Status.UNKNOWN, future.status());
        assertEquals("ERR", future.signal());
        assertEquals("future_timestamp", future.reason());
    }
}
