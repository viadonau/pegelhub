package com.stm.pegelhub.connector.api;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ConnectorDtoTest {
    private static final String LONG_DATA = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz";

    private static final UUID id = UUID.randomUUID();

    @Test
    void constructor_WhenEverythingWorks() {
        assertDoesNotThrow(() -> new ConnectorDto(id, "nr", null, "desc", "1.0",
                "1.01", "def", null, null, null, "notes"));
    }

    @Test
    void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new ConnectorDto(null, "nr", null, "desc", "1.0",
                "1.01", "def", null, null, null, "notes"));
        assertThrows(NullPointerException.class, () -> new ConnectorDto(id, null, null, "desc", "1.0",
                "1.01", "def", null, null, null, "notes"));
        assertThrows(NullPointerException.class, () -> new ConnectorDto(id, "nr", null, null, "1.0",
                "1.01", "def", null, null, null, "notes"));
        assertThrows(NullPointerException.class, () -> new ConnectorDto(id, "nr", null, "desc", null,
                "1.01", "def", null, null, null, "notes"));
        assertThrows(NullPointerException.class, () -> new ConnectorDto(id, "nr", null, "desc", "1.0",
                null, "def", null, null, null, "notes"));
        assertThrows(NullPointerException.class, () -> new ConnectorDto(id, "nr", null, "desc", "1.0",
                "1.01", null, null, null, null, "notes"));
        assertThrows(NullPointerException.class, () -> new ConnectorDto(id, "nr", null, "desc", "1.0",
                "1.01", "def", null, null, null, null));
    }

    @Test
    void constructorWithEmptyArgsThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> new ConnectorDto(id, "", null, "desc", "1.0",
                "1.01", "def", null, null, null, "notes"));
    }

    @Test
    void constructorWithLongArgsThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> new ConnectorDto(id, LONG_DATA, null, "desc", "1.0",
                "1.01", "def", null, null, null, "notes"));
        assertThrows(IllegalArgumentException.class, () -> new ConnectorDto(id, "nr", null, LONG_DATA, "1.0",
                "1.01", "def", null, null, null, "notes"));
        assertThrows(IllegalArgumentException.class, () -> new ConnectorDto(id, "nr", null, "desc", LONG_DATA,
                "1.01", "def", null, null, null, "notes"));
        assertThrows(IllegalArgumentException.class, () -> new ConnectorDto(id, "nr", null, "desc", "1.0",
                LONG_DATA, "def", null, null, null, "notes"));
        assertThrows(IllegalArgumentException.class, () -> new ConnectorDto(id, "nr", null, "desc", "1.0",
                "1.01", LONG_DATA, null, null, null, "notes"));
        assertThrows(IllegalArgumentException.class, () -> new ConnectorDto(id, "nr", null, "desc", "1.0",
                "1.01", "def", null, null, null, LONG_DATA));
    }
}