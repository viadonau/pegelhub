package com.stm.pegelhub.supplier.api;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StationManufacturerDtoTest {
    private static final String LONG_DATA = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz";

    private static final UUID id = UUID.randomUUID();

    @Test
    void constructor_WhenEverythingWorks() {
        assertDoesNotThrow(() -> new StationManufacturerDto(id, "name", "type",
                "1.01", "rem"));
    }

    @Test
    void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new StationManufacturerDto(null, "name", "type",
                "1.01", "rem"));
        assertThrows(NullPointerException.class, () -> new StationManufacturerDto(id, null, "type",
                "1.01", "rem"));
        assertThrows(NullPointerException.class, () -> new StationManufacturerDto(id, "name", null,
                "1.01", "rem"));
        assertThrows(NullPointerException.class, () -> new StationManufacturerDto(id, "name", "type",
                null, "rem"));
        assertThrows(NullPointerException.class, () -> new StationManufacturerDto(id, "name", "type",
                "1.01", null));
    }

    @Test
    void constructorWithLongArgsThrowsIAE() {

        assertThrows(IllegalArgumentException.class, () -> new StationManufacturerDto(id, LONG_DATA, "type",
                "1.01", "rem"));
        assertThrows(IllegalArgumentException.class, () -> new StationManufacturerDto(id, "name", LONG_DATA,
                "1.01", "rem"));
        assertThrows(IllegalArgumentException.class, () -> new StationManufacturerDto(id, "name", "type",
                LONG_DATA, "rem"));
        assertThrows(IllegalArgumentException.class, () -> new StationManufacturerDto(id, "name", "type",
                "1.01", LONG_DATA));
    }
}