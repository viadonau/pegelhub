package at.pegelhub.measurement.api;

import at.pegelhub.measurement.api.write.WriteMeasurementRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WriteMeasurementRequestTest {

    private static final UUID TIME_SERIES_ID = UUID.fromString("8ce8c5b6-f093-4d46-b770-7239cdfa3d76");
    private static final Instant OBSERVED_AT = Instant.parse("2026-04-25T10:15:30Z");

    @Test
    void constructorAcceptsValidMeasurement() {
        assertDoesNotThrow(() -> new WriteMeasurementRequest(TIME_SERIES_ID, OBSERVED_AT, 10.5));
    }

    @Test
    void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new WriteMeasurementRequest(null, OBSERVED_AT, 10.5));
        assertThrows(NullPointerException.class, () -> new WriteMeasurementRequest(TIME_SERIES_ID, null, 10.5));
        assertThrows(NullPointerException.class, () -> new WriteMeasurementRequest(TIME_SERIES_ID, OBSERVED_AT, null));
    }
}
