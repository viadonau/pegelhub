package at.pegelhub.telemetry.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class TelemetryTest {


    private static final String MEASUREMENT = "measurement";
    private static final String IP_ADDRESS = "172.0.0.0";
    private static final Instant TIMESTAMP = Instant.parse("2022-02-10T10:30:00Z");
    private static final int CYCLE_TIME = 1;
    private static final int INVALID_CYCLE_TIME = -2;
    private static final double TEMPERATURE = -2.0;
    private static final double PERFORMANCE = 2.0;
    private static final double INVALID_PERFORMANCE = -2.0;
    private static final double FIELD_STRENGTH = 2.0;
    private static final double INVALID_FIELD_STRENGTH = -2.0;

    @Test
    public void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new Telemetry(null, IP_ADDRESS, IP_ADDRESS,
                TIMESTAMP, CYCLE_TIME, TEMPERATURE, TEMPERATURE, PERFORMANCE, PERFORMANCE,
                PERFORMANCE, PERFORMANCE, FIELD_STRENGTH));
        assertThrows(NullPointerException.class, () -> new Telemetry(MEASUREMENT, null, IP_ADDRESS,
                TIMESTAMP, CYCLE_TIME, TEMPERATURE, TEMPERATURE, PERFORMANCE, PERFORMANCE,
                PERFORMANCE, PERFORMANCE, FIELD_STRENGTH));
        assertThrows(NullPointerException.class, () -> new Telemetry(MEASUREMENT, IP_ADDRESS, null,
                TIMESTAMP, CYCLE_TIME, TEMPERATURE, TEMPERATURE, PERFORMANCE, PERFORMANCE,
                PERFORMANCE, PERFORMANCE, FIELD_STRENGTH));
        assertThrows(NullPointerException.class, () -> new Telemetry(MEASUREMENT, IP_ADDRESS, IP_ADDRESS,
                null, CYCLE_TIME, TEMPERATURE, TEMPERATURE, PERFORMANCE, PERFORMANCE,
                PERFORMANCE, PERFORMANCE, FIELD_STRENGTH));
    }

    @Test
    public void constructorWithNegativeArgThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> new Telemetry(MEASUREMENT, IP_ADDRESS, IP_ADDRESS,
                TIMESTAMP, INVALID_CYCLE_TIME, TEMPERATURE, TEMPERATURE, PERFORMANCE, PERFORMANCE,
                PERFORMANCE, PERFORMANCE, FIELD_STRENGTH));
        assertThrows(IllegalArgumentException.class, () -> new Telemetry(MEASUREMENT, IP_ADDRESS, IP_ADDRESS,
                TIMESTAMP, CYCLE_TIME, TEMPERATURE, TEMPERATURE, INVALID_PERFORMANCE, PERFORMANCE,
                PERFORMANCE, PERFORMANCE, FIELD_STRENGTH));
        assertThrows(IllegalArgumentException.class, () -> new Telemetry(MEASUREMENT, IP_ADDRESS, IP_ADDRESS,
                TIMESTAMP, CYCLE_TIME, TEMPERATURE, TEMPERATURE, PERFORMANCE, INVALID_PERFORMANCE,
                PERFORMANCE, PERFORMANCE, FIELD_STRENGTH));
        assertThrows(IllegalArgumentException.class, () -> new Telemetry(MEASUREMENT, IP_ADDRESS, IP_ADDRESS,
                TIMESTAMP, CYCLE_TIME, TEMPERATURE, TEMPERATURE, PERFORMANCE, PERFORMANCE,
                INVALID_PERFORMANCE, PERFORMANCE, FIELD_STRENGTH));
        assertThrows(IllegalArgumentException.class, () -> new Telemetry(MEASUREMENT, IP_ADDRESS, IP_ADDRESS,
                TIMESTAMP, CYCLE_TIME, TEMPERATURE, TEMPERATURE, PERFORMANCE, PERFORMANCE,
                PERFORMANCE, INVALID_PERFORMANCE, FIELD_STRENGTH));
        assertThrows(IllegalArgumentException.class, () -> new Telemetry(MEASUREMENT, IP_ADDRESS, IP_ADDRESS,
                TIMESTAMP, CYCLE_TIME, TEMPERATURE, TEMPERATURE, PERFORMANCE, PERFORMANCE,
                PERFORMANCE, PERFORMANCE, INVALID_FIELD_STRENGTH));
    }

    @Test
    public void constructorDoesNotThrow() {
        assertDoesNotThrow(() -> new Telemetry(MEASUREMENT, IP_ADDRESS, IP_ADDRESS,
                TIMESTAMP, CYCLE_TIME, TEMPERATURE, TEMPERATURE, PERFORMANCE, PERFORMANCE,
                PERFORMANCE, PERFORMANCE, FIELD_STRENGTH));
    }
}
