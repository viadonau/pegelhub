package com.stm.pegelhub.taker.api;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TakerServiceManufacturerDtoTest {
    private static final String LONG_DATA = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz";

    private static final UUID id = UUID.randomUUID();

    @Test
    void constructor_WhenEverythingWorks() {
        assertDoesNotThrow(() -> new TakerServiceManufacturerDto(id, "name", "type",
                "1.01", "rem"));
    }

    @Test
    void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new TakerServiceManufacturerDto(null, "name", "type",
                "1.01", "rem"));
        assertThrows(NullPointerException.class, () -> new TakerServiceManufacturerDto(id, null, "type",
                "1.01", "rem"));
        assertThrows(NullPointerException.class, () -> new TakerServiceManufacturerDto(id, "name", null,
                "1.01", "rem"));
        assertThrows(NullPointerException.class, () -> new TakerServiceManufacturerDto(id, "name", "type",
                null, "rem"));
        assertThrows(NullPointerException.class, () -> new TakerServiceManufacturerDto(id, "name", "type",
                "1.01", null));
    }

    @Test
    void constructorWithLongArgsThrowsIAE() {

        assertThrows(IllegalArgumentException.class, () -> new TakerServiceManufacturerDto(id, LONG_DATA, "type",
                "1.01", "rem"));
        assertThrows(IllegalArgumentException.class, () -> new TakerServiceManufacturerDto(id, "name", LONG_DATA,
                "1.01", "rem"));
        assertThrows(IllegalArgumentException.class, () -> new TakerServiceManufacturerDto(id, "name", "type",
                LONG_DATA, "rem"));
        assertThrows(IllegalArgumentException.class, () -> new TakerServiceManufacturerDto(id, "name", "type",
                "1.01", LONG_DATA));
    }
}