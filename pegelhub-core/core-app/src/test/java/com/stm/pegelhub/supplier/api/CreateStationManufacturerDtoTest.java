package com.stm.pegelhub.supplier.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CreateStationManufacturerDtoTest {
    private static final String LONG_DATA = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz";

    @Test
    void constructor_WhenEverythingWorks() {
        assertDoesNotThrow(() -> new CreateStationManufacturerDto("name", "type",
                "1.01", "rem"));
    }

    @Test
    void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new CreateStationManufacturerDto(null, "type",
                "1.01", "rem"));
        assertThrows(NullPointerException.class, () -> new CreateStationManufacturerDto("name", null,
                "1.01", "rem"));
        assertThrows(NullPointerException.class, () -> new CreateStationManufacturerDto("name", "type",
                null, "rem"));
        assertThrows(NullPointerException.class, () -> new CreateStationManufacturerDto("name", "type",
                "1.01", null));
    }

    @Test
    void constructorWithLongArgsThrowsIAE() {

        assertThrows(IllegalArgumentException.class, () -> new CreateStationManufacturerDto(LONG_DATA, "type",
                "1.01", "rem"));
        assertThrows(IllegalArgumentException.class, () -> new CreateStationManufacturerDto("name", LONG_DATA,
                "1.01", "rem"));
        assertThrows(IllegalArgumentException.class, () -> new CreateStationManufacturerDto("name", "type",
                LONG_DATA, "rem"));
        assertThrows(IllegalArgumentException.class, () -> new CreateStationManufacturerDto("name", "type",
                "1.01", LONG_DATA));
    }
}