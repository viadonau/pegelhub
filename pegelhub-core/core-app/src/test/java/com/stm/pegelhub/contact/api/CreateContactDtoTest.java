package com.stm.pegelhub.contact.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class CreateContactDtoTest {
    @Test
    void constructor_WhenEverythingWorks() {
        assertDoesNotThrow(() -> new CreateContactDto(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));
    }
}