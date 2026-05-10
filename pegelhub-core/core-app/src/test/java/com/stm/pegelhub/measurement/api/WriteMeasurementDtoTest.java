package com.stm.pegelhub.measurement.api;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WriteMeasurementDtoTest {

    @Test
    void constructor_WhenEverythingWorks() {
        assertDoesNotThrow(() -> new WriteMeasurementDto(LocalDateTime.now(), Map.of("hello", 1.0), Map.of()));
    }

    @Test
    void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new WriteMeasurementDto(null, Map.of("hello", 1.0), Map.of()));
        assertThrows(NullPointerException.class, () -> new WriteMeasurementDto(LocalDateTime.now(), null, Map.of()));
        assertThrows(NullPointerException.class, () -> new WriteMeasurementDto(LocalDateTime.now(), Map.of("hello", 1.0), null));
    }

    @Test
    void constructorWithEmptyArgsThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> new WriteMeasurementDto(LocalDateTime.now(), Map.of(), Map.of()));
    }
}