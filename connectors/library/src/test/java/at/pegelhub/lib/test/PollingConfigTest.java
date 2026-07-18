package at.pegelhub.lib.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PollingConfigTest {
    @Test
    void requiresAnInterval() {
        assertThrows(IllegalArgumentException.class, () -> new PollingConfig(null));
        assertThrows(IllegalArgumentException.class, () -> new PollingConfig(" "));
    }

    @Test
    void parsesSupportedIntervals() {
        assertEquals(Duration.ofSeconds(45), new PollingConfig("45s").duration());
        assertEquals(Duration.ofMinutes(30), new PollingConfig("30m").duration());
        assertEquals(Duration.ofHours(1), new PollingConfig("1h").duration());
    }

    @Test
    void rejectsInvalidIntervals() {
        assertThrows(IllegalArgumentException.class, () -> new PollingConfig("0s").duration());
        assertThrows(IllegalArgumentException.class, () -> new PollingConfig("10x").duration());
        assertThrows(IllegalArgumentException.class, () -> new PollingConfig("seconds").duration());
    }
}
