package com.stm.pegelhub.auth.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiTokenDtoTest {

    @Test
    void constructor_WhenEverythingWorks() {
        assertDoesNotThrow(() -> new ApiTokenDto("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")); // 64 long
    }
    @Test
    void constructor_WhenApiKeyLengthIncorrect_ThrowsIAE() { // IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> new ApiTokenDto("A")); // 1 long
        assertThrows(IllegalArgumentException.class, () -> new ApiTokenDto("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")); // 65 long
    }
}