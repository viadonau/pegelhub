package com.stm.pegelhub.shared.influx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabasePropertiesTest {

    @Test
    public void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new DatabaseProperties(null, "org", "bucket", "token"));
        assertThrows(NullPointerException.class, () -> new DatabaseProperties("url", null, "bucket", "token"));
        assertThrows(NullPointerException.class, () -> new DatabaseProperties("url", "org", null, "token"));
        assertThrows(NullPointerException.class, () -> new DatabaseProperties("url", "org", "bucket", null));
    }

    @Test
    public void constructorWithEmptyArgsThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> new DatabaseProperties("", "org", "bucket", "token"));
        assertThrows(IllegalArgumentException.class, () -> new DatabaseProperties("url", "", "bucket", "token"));
        assertThrows(IllegalArgumentException.class, () -> new DatabaseProperties("url", "org", "", "token"));
        assertThrows(IllegalArgumentException.class, () -> new DatabaseProperties("url", "org", "bucket", ""));
    }

    @Test
    public void constructorDoseNotThrow() {
        assertDoesNotThrow(() -> new DatabaseProperties("url", "org", "bucket", "token"));
    }
}