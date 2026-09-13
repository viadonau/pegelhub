package at.pegelhub.connector.ma.core;

import at.pegelhub.lib.config.ConfigValidation;
import at.pegelhub.lib.config.DirectedMapping;
import at.pegelhub.lib.config.MappingDirection;

import java.util.Objects;
import java.util.UUID;

public record InputMapping(
        String revInput,
        UUID timeSeriesId,
        MappingDirection direction
) implements DirectedMapping {
    public InputMapping {
        revInput = ConfigValidation.requireText(revInput, "revInput");
        Objects.requireNonNull(timeSeriesId, "timeSeriesId");
        Objects.requireNonNull(direction, "direction");
    }
}
