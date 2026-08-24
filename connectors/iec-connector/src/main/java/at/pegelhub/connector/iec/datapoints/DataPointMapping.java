package at.pegelhub.connector.iec.datapoints;

import at.pegelhub.lib.config.DirectedMapping;
import at.pegelhub.lib.config.MappingDirection;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record DataPointMapping(
        Integer iecIoa,
        UUID timeSeriesId,
        MappingDirection direction,
        BigDecimal gaugeZeroElevationMAboveAdria
) implements DirectedMapping {
    public DataPointMapping(Integer iecIoa, UUID timeSeriesId, MappingDirection direction) {
        this(iecIoa, timeSeriesId, direction, null);
    }

    public DataPointMapping {
        Objects.requireNonNull(iecIoa, "iecIoa");
        Objects.requireNonNull(timeSeriesId, "timeSeriesId");
        Objects.requireNonNull(direction, "direction");
        if (gaugeZeroElevationMAboveAdria != null && direction != MappingDirection.CORE_TO_EXTERNAL) {
            throw new IllegalArgumentException(
                    "gaugeZeroElevationMAboveAdria is only supported for core-to-external mappings");
        }
    }
}
