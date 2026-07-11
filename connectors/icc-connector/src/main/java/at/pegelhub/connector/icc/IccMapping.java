package at.pegelhub.connector.icc;

import at.pegelhub.lib.config.DirectedMapping;
import at.pegelhub.lib.config.MappingDirection;

import java.util.Objects;
import java.util.UUID;

record IccMapping(UUID timeSeriesId, UUID externalTimeSeriesId, MappingDirection direction) implements DirectedMapping {
    IccMapping {
        Objects.requireNonNull(timeSeriesId, "timeSeriesId");
        Objects.requireNonNull(externalTimeSeriesId, "externalTimeSeriesId");
        Objects.requireNonNull(direction, "direction");
    }
}
