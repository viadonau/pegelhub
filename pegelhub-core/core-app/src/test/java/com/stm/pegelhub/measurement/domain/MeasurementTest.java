package com.stm.pegelhub.measurement.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class MeasurementTest {

    private static final UUID MEASUREMENT = UUID.fromString("d55f58b1-ac1d-4a52-b1bb-7e2ec51dfb31");
    private static final LocalDateTime TIMESTAMP = LocalDateTime.parse("2022-02-10T10:30:00");
    private static final Map<String, Double> FIELDS = Map.of("key1", 1.0);
    private static final Map<String, String> INFOS = Map.of("key1", "value1");
    private static final Map<String, Double> EMPTY_MAP = Map.of();

    @Test
    public void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new Measurement(null, TIMESTAMP,
                FIELDS, INFOS));
        assertThrows(NullPointerException.class, () -> new Measurement(MEASUREMENT, null,
                FIELDS, INFOS));
        assertThrows(NullPointerException.class, () -> new Measurement(MEASUREMENT, TIMESTAMP,
                null, INFOS));
        assertThrows(NullPointerException.class, () -> new Measurement(MEASUREMENT, TIMESTAMP,
                FIELDS, null));
    }

    @Test
    public void constructorWithEmptyMapsThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> new Measurement(MEASUREMENT, TIMESTAMP,
                EMPTY_MAP, INFOS));
    }

    @Test
    public void constructorDoesNotThrow() {
        assertDoesNotThrow(() -> new Measurement(MEASUREMENT, TIMESTAMP, FIELDS, INFOS));
    }
}