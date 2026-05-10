package at.pegelhub.connector.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CreateConnectorDtoTest {
    private static final String LONG_DATA = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz";

    @Test
    void constructor_WhenEverythingWorks() {
        assertDoesNotThrow(() -> new CreateConnectorDto("nr", null, "desc", "1.0",
                "1.01", "def", null, null, null, "notes", null));
    }

    @Test
    void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new CreateConnectorDto(null, null, "desc", "1.0",
                "1.01", "def", null, null, null, "notes", null));
        assertThrows(NullPointerException.class, () -> new CreateConnectorDto("nr", null, null, "1.0",
                "1.01", "def", null, null, null, "notes", null));
        assertThrows(NullPointerException.class, () -> new CreateConnectorDto("nr", null, "desc", null,
                "1.01", "def", null, null, null, "notes", null));
        assertThrows(NullPointerException.class, () -> new CreateConnectorDto("nr", null, "desc", "1.0",
                null, "def", null, null, null, "notes", null));
        assertThrows(NullPointerException.class, () -> new CreateConnectorDto("nr", null, "desc", "1.0",
                "1.01", null, null, null, null, "notes", null));
        assertThrows(NullPointerException.class, () -> new CreateConnectorDto("nr", null, "desc", "1.0",
                "1.01", "def", null, null, null, null, null));
    }

    @Test
    void constructorWithEmptyArgsThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> new CreateConnectorDto("", null, "desc", "1.0",
                "1.01", "def", null, null, null, "notes", null));
    }

    @Test
    void constructorWithLongArgsThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> new CreateConnectorDto(LONG_DATA, null, "desc", "1.0",
                "1.01", "def", null, null, null, "notes", null));
        assertThrows(IllegalArgumentException.class, () -> new CreateConnectorDto("nr", null, LONG_DATA, "1.0",
                "1.01", "def", null, null, null, "notes", null));
        assertThrows(IllegalArgumentException.class, () -> new CreateConnectorDto("nr", null, "desc", LONG_DATA,
                "1.01", "def", null, null, null, "notes", null));
        assertThrows(IllegalArgumentException.class, () -> new CreateConnectorDto("nr", null, "desc", "1.0",
                LONG_DATA, "def", null, null, null, "notes", null));
        assertThrows(IllegalArgumentException.class, () -> new CreateConnectorDto("nr", null, "desc", "1.0",
                "1.01", LONG_DATA, null, null, null, "notes", null));
        assertThrows(IllegalArgumentException.class, () -> new CreateConnectorDto("nr", null, "desc", "1.0",
                "1.01", "def", null, null, null, LONG_DATA, null));
    }
}