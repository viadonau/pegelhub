package at.pegelhub.connector.ma.core;

import at.pegelhub.lib.config.DirectedMapping;
import at.pegelhub.lib.config.MappingDirection;

import java.util.Objects;
import java.util.UUID;

public record InputMapping(String revInput, UUID timeSeriesId, MappingDirection direction) implements DirectedMapping {
    public InputMapping {
        revInput = requireText(revInput, "revInput");
        Objects.requireNonNull(timeSeriesId, "timeSeriesId");
        Objects.requireNonNull(direction, "direction");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
