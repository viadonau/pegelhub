package at.pegelhub.connector.iec.datapoints;

import at.pegelhub.lib.config.DirectedMapping;
import at.pegelhub.lib.config.MappingDirection;

import java.util.Objects;
import java.util.UUID;

public record DataPointMapping(
        Integer iecIoa,
        UUID timeSeriesId,
        MappingDirection direction
) implements DirectedMapping {
    public DataPointMapping {
        Objects.requireNonNull(iecIoa, "iecIoa");
        Objects.requireNonNull(timeSeriesId, "timeSeriesId");
        Objects.requireNonNull(direction, "direction");
    }
}
