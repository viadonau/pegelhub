package at.pegelhub.lib.config;

import org.junit.jupiter.api.Test;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScheduleConfigTest {
    @Test
    void shouldRequireDelay() {
        assertThrows(IllegalArgumentException.class, () -> new ScheduleConfig(null));
        assertThrows(IllegalArgumentException.class, () -> new ScheduleConfig(" "));
    }

    @Test
    void shouldTrimDelay() {
        assertEquals("30s", new ScheduleConfig(" 30s ").delay());
    }

    @Test
    void parsesSupportedIntervals() {
        assertEquals(Duration.ofSeconds(45), new ScheduleConfig("45s").interval());
        assertEquals(Duration.ofMinutes(30), new ScheduleConfig("30m").interval());
        assertEquals(Duration.ofHours(1), new ScheduleConfig("1h").interval());
    }

    @Test
    void rejectsInvalidIntervals() {
        assertThrows(IllegalArgumentException.class, () -> new ScheduleConfig("0s").interval());
        assertThrows(IllegalArgumentException.class, () -> new ScheduleConfig("10x").interval());
        assertThrows(IllegalArgumentException.class, () -> new ScheduleConfig("seconds").interval());
    }
}
