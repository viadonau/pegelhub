package at.pegelhub.lib.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowedPollingConfigTest {
    @Test
    void defaultsOnlyTheOverlap() {
        WindowedPollingConfig config = new WindowedPollingConfig("15m", null);
        assertEquals(Duration.ofMinutes(15), config.duration());
        assertEquals(Duration.ofHours(1), config.overlapDuration());
        assertThrows(IllegalArgumentException.class, () -> new WindowedPollingConfig(null, null).duration());
    }

    @Test
    void reusesDurationLiterals() {
        assertEquals(Duration.ofSeconds(30), new WindowedPollingConfig("5m", "30s").overlapDuration());
        assertEquals(Duration.ofMinutes(90), new WindowedPollingConfig("5m", " 90M ").overlapDuration());
        assertEquals(Duration.ofHours(2), new WindowedPollingConfig("5m", "2h").overlapDuration());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "0s", "-1h", "1.5h", "10x", "seconds", "999999999999999999999h"})
    void rejectsInvalidOverlapWithItsConfigurationName(String overlap) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new WindowedPollingConfig("5m", overlap).overlapDuration());
        assertTrue(error.getMessage().contains("polling.overlap"));
    }
}
