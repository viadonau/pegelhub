package at.pegelhub.taker.api;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static at.pegelhub.testsupport.ExampleDtos.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TakerDtoTest {
    private static final String LONG_DATA = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz";

    private static final UUID id = UUID.randomUUID();

    @Test
    void constructor_WhenEverythingWorks() {
        assertDoesNotThrow(() -> new TakerDto(id, "number", 10, TAKER_SERVICE_MANUFACTURER_DTO,
                CONNECTOR_DTO, REFRESH_RATE));
    }

    @Test
    void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new TakerDto(null, "number", 10, TAKER_SERVICE_MANUFACTURER_DTO,
                CONNECTOR_DTO, REFRESH_RATE));
        assertThrows(NullPointerException.class, () -> new TakerDto(id, null, 10, TAKER_SERVICE_MANUFACTURER_DTO,
                CONNECTOR_DTO, REFRESH_RATE));
        assertThrows(NullPointerException.class, () -> new TakerDto(id, "number", null, TAKER_SERVICE_MANUFACTURER_DTO,
                CONNECTOR_DTO, REFRESH_RATE));
        assertThrows(NullPointerException.class, () -> new TakerDto(id, "number", 10, null,
                CONNECTOR_DTO, REFRESH_RATE));
        assertThrows(NullPointerException.class, () -> new TakerDto(id, "number", 10, TAKER_SERVICE_MANUFACTURER_DTO,
                null, REFRESH_RATE));
        assertThrows(NullPointerException.class, () -> new TakerDto(id, "number", 10, TAKER_SERVICE_MANUFACTURER_DTO,
                CONNECTOR_DTO, null));
    }

    @Test
    void constructorWithLongArgsThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> new TakerDto(id, LONG_DATA, 10, TAKER_SERVICE_MANUFACTURER_DTO,
                CONNECTOR_DTO, REFRESH_RATE));
    }

    @Test
    void constructorWithEmptyArgsThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> new TakerDto(id, "", 10, TAKER_SERVICE_MANUFACTURER_DTO,
                CONNECTOR_DTO, REFRESH_RATE));
    }
}