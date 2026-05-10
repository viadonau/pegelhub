package com.stm.pegelhub.shared.error;

/**
 * Exception for objects that were requested but do not exist.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
