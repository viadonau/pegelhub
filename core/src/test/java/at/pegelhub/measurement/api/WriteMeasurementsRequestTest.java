package at.pegelhub.measurement.api;

import at.pegelhub.measurement.api.write.WriteMeasurementRequest;
import at.pegelhub.measurement.api.write.WriteMeasurementsRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WriteMeasurementsRequestTest {

    private static final UUID TIME_SERIES_ID = UUID.fromString("8ce8c5b6-f093-4d46-b770-7239cdfa3d76");

    @Test
    void constructorAcceptsValidMeasurements() {
        assertDoesNotThrow(() -> new WriteMeasurementsRequest(List.of(
                new WriteMeasurementRequest(TIME_SERIES_ID, Instant.now(), 10.5))));
    }

    @Test
    void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new WriteMeasurementsRequest(null));
    }

    @Test
    void constructorWithEmptyArgsThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> new WriteMeasurementsRequest(List.of()));
    }
}
