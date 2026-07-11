package at.pegelhub.lib.config;

import org.junit.jupiter.api.Test;

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
}
