package com.stm.pegelhub.shared.web;

import org.springframework.core.convert.converter.Converter;

import java.util.UUID;

import static java.util.Objects.requireNonNull;


/**
 * Converter, which takes an incoming String and makes a UUID out of it.
 */
public class StringToUUIDConverter implements Converter<String, UUID> {
    public UUID convert(String source) {
        return UUID.fromString(requireNonNull(source));
    }
}