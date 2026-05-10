package com.stm.pegelhub.taker.api;

import org.junit.jupiter.api.Test;

import static com.stm.pegelhub.testsupport.ExampleDtos.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CreateTakerDtoTest {
    private static final String LONG_DATA = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz";

    @Test
    void constructor_WhenEverythingWorks() {
        assertDoesNotThrow(() -> new CreateTakerDto("number", 10, CREATE_TAKER_SERVICE_MANUFACTURER_DTO,
                CREATE_CONNECTOR_DTO, REFRESH_RATE));
    }

    @Test
    void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new CreateTakerDto(null, 10, CREATE_TAKER_SERVICE_MANUFACTURER_DTO,
                CREATE_CONNECTOR_DTO, REFRESH_RATE));
        assertThrows(NullPointerException.class, () -> new CreateTakerDto("number", null, CREATE_TAKER_SERVICE_MANUFACTURER_DTO,
                CREATE_CONNECTOR_DTO, REFRESH_RATE));
        assertThrows(NullPointerException.class, () -> new CreateTakerDto("number", 10, null,
                CREATE_CONNECTOR_DTO, REFRESH_RATE));
        assertThrows(NullPointerException.class, () -> new CreateTakerDto("number", 10, CREATE_TAKER_SERVICE_MANUFACTURER_DTO,
                null, REFRESH_RATE));
        assertThrows(NullPointerException.class, () -> new CreateTakerDto("number", 10, CREATE_TAKER_SERVICE_MANUFACTURER_DTO,
                CREATE_CONNECTOR_DTO, null));
    }

    @Test
    void constructorWithLongArgsThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> new CreateTakerDto(LONG_DATA, 10, CREATE_TAKER_SERVICE_MANUFACTURER_DTO,
                CREATE_CONNECTOR_DTO, REFRESH_RATE));
    }

    @Test
    void constructorWithEmptyArgsThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> new CreateTakerDto("", 10, CREATE_TAKER_SERVICE_MANUFACTURER_DTO,
                CREATE_CONNECTOR_DTO, REFRESH_RATE));
    }
}