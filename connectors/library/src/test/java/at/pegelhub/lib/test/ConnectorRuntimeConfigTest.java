package at.pegelhub.lib.runtime;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConnectorRuntimeConfigTest {
    @Test
    void resolvesDefaultConfigDirWhenArgsAreEmpty() {
        ConnectorRuntimeConfig config = ConnectorRuntimeConfig.fromArgs(new String[0]);

        assertEquals(ConnectorRuntimeConfig.DEFAULT_CONFIG_DIR, config.configDir().toString());
    }

    @Test
    void parsesDurationWithCommonUnits() {
        assertEquals(Duration.ofHours(1), ConnectorRuntimeConfig.parseDuration("1h"));
        assertEquals(Duration.ofMinutes(30), ConnectorRuntimeConfig.parseDuration("30m"));
        assertEquals(Duration.ofSeconds(45), ConnectorRuntimeConfig.parseDuration("45s"));
    }

    @Test
    void rejectsMissingDuration() {
        assertThrows(IllegalArgumentException.class, () -> ConnectorRuntimeConfig.parseDuration(null));
        assertThrows(IllegalArgumentException.class, () -> ConnectorRuntimeConfig.parseDuration(""));
    }

    @Test
    void rejectsUnknownDurationUnit() {
        assertThrows(IllegalArgumentException.class, () -> ConnectorRuntimeConfig.parseDuration("10x"));
    }
}
