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
        String unit
) implements DirectedMapping {
    public TstpMapping {
        Objects.requireNonNull(timeSeriesId, "timeSeriesId");
        Objects.requireNonNull(direction, "direction");
        parameter = parameter == null ? TstpParameter.WATER_LEVEL : parameter;
        unit = unit == null ? parameter.defaultUnit() : unit;
        parameter.representation(unit);
    }
}
