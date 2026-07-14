package at.pegelhub.connector.tstp;

import at.pegelhub.lib.config.DirectedMapping;
import at.pegelhub.lib.config.MappingDirection;

import java.util.Objects;
import java.util.UUID;

record TstpMapping(
        UUID timeSeriesId,
        Integer stationId,
        MappingDirection direction,
        boolean verifyRoundTrip) implements DirectedMapping {
    TstpMapping {
        Objects.requireNonNull(timeSeriesId, "timeSeriesId");
        Objects.requireNonNull(stationId, "stationId");
        Objects.requireNonNull(direction, "direction");
    }
}
