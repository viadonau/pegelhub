package at.pegelhub.taker.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CreateTakerServiceManufacturerDtoTest {
    private static final String LONG_DATA = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz";

    @Test
    void constructor_WhenEverythingWorks() {
        assertDoesNotThrow(() -> new CreateTakerServiceManufacturerDto("name", "type",
                "1.01", "rem"));
    }

    @Test
    void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new CreateTakerServiceManufacturerDto(null, "type",
                "1.01", "rem"));
        assertThrows(NullPointerException.class, () -> new CreateTakerServiceManufacturerDto("name", null,
                "1.01", "rem"));
        assertThrows(NullPointerException.class, () -> new CreateTakerServiceManufacturerDto("name", "type",
                null, "rem"));
        assertThrows(NullPointerException.class, () -> new CreateTakerServiceManufacturerDto("name", "type",
                "1.01", null));
    }

    @Test
    void constructorWithLongArgsThrowsIAE() {

        assertThrows(IllegalArgumentException.class, () -> new CreateTakerServiceManufacturerDto(LONG_DATA, "type",
                "1.01", "rem"));
        assertThrows(IllegalArgumentException.class, () -> new CreateTakerServiceManufacturerDto("name", LONG_DATA,
                "1.01", "rem"));
        assertThrows(IllegalArgumentException.class, () -> new CreateTakerServiceManufacturerDto("name", "type",
                LONG_DATA, "rem"));
        assertThrows(IllegalArgumentException.class, () -> new CreateTakerServiceManufacturerDto("name", "type",
                "1.01", LONG_DATA));
    }
}