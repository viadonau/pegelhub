package at.pegelhub.measurement.api;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WriteMeasurementsDtoTest {

    private static final UUID TIME_SERIES_ID = UUID.fromString("8ce8c5b6-f093-4d46-b770-7239cdfa3d76");

    @Test
    void constructor_WhenEverythingWorks() {
        assertDoesNotThrow(() -> new WriteMeasurementsDto(List.of(new WriteMeasurementDto(TIME_SERIES_ID, Instant.now(), 10.5))));
    }

    @Test
    void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new WriteMeasurementsDto(null));
    }

    @Test
    void constructorWithEmptyArgsThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> new WriteMeasurementsDto(List.of()));
    }
}
