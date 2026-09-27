package at.pegelhub.connector.tstp;

import at.pegelhub.lib.config.DirectedMapping;
import at.pegelhub.lib.config.MappingDirection;

import java.util.Objects;
import java.util.UUID;

public record TstpMapping(
        UUID timeSeriesId,
        int stationId,
        MappingDirection direction,
        TstpParameter parameter,
        String unit,
        TstpMeanExport meanExport
) implements DirectedMapping {
    public TstpMapping(UUID timeSeriesId, int stationId, MappingDirection direction,
                       TstpParameter parameter, String unit) {
        this(timeSeriesId, stationId, direction, parameter, unit, null);
    }

    public TstpMapping {
        Objects.requireNonNull(timeSeriesId, "timeSeriesId");
        Objects.requireNonNull(direction, "direction");
        parameter = parameter == null ? TstpParameter.WATER_LEVEL : parameter;
        unit = unit == null ? parameter.defaultUnit() : unit;
        parameter.representation(unit);
        if (meanExport != null && direction != MappingDirection.CORE_TO_EXTERNAL) {
            throw new IllegalArgumentException("meanExport is only valid for core-to-external mappings");
        }
    }
}
