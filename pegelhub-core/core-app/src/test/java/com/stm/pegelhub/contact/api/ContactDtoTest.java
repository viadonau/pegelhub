package com.stm.pegelhub.contact.api;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ContactDtoTest {

    private static final UUID id = UUID.randomUUID();

    @Test
    void constructor_WhenEverythingWorks() {
        assertDoesNotThrow(() -> new ContactDto(id, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));
    }

    @Test
    void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new ContactDto(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));
    }
}