package at.pegelhub.connector.tstp;

import at.pegelhub.lib.config.DirectedMapping;
import at.pegelhub.lib.config.MappingDirection;

import java.util.Objects;
import java.util.UUID;

public record TstpMapping(
        UUID timeSeriesId,
        int stationId,
        MappingDirection direction
) implements DirectedMapping {
    public TstpMapping {
        Objects.requireNonNull(timeSeriesId, "timeSeriesId");
        Objects.requireNonNull(direction, "direction");
    }
}
