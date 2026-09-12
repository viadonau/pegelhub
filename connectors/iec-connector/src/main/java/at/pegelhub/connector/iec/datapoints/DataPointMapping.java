package at.pegelhub.connector.iec.datapoints;

import at.pegelhub.lib.config.DirectedMapping;
import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.model.MeasurementRepresentation;

import java.util.Objects;
import java.util.UUID;

public record DataPointMapping(
        Integer iecIoa,
        UUID timeSeriesId,
        MappingDirection direction,
        MeasurementRepresentation outputRepresentation) implements DirectedMapping {
    public DataPointMapping(Integer iecIoa, UUID timeSeriesId, MappingDirection direction) {
        this(iecIoa, timeSeriesId, direction, MeasurementRepresentation.CANONICAL);
    }

    public DataPointMapping {
        Objects.requireNonNull(iecIoa, "iecIoa");
        Objects.requireNonNull(timeSeriesId, "timeSeriesId");
        Objects.requireNonNull(direction, "direction");
        outputRepresentation = outputRepresentation == null
                ? MeasurementRepresentation.CANONICAL
                : outputRepresentation;
        if (outputRepresentation != MeasurementRepresentation.CANONICAL
                && direction != MappingDirection.CORE_TO_EXTERNAL) {
            throw new IllegalArgumentException(
                    "outputRepresentation is only supported for core-to-external mappings; "
                            + "configure inbound representation in Core");
        }
    }
}
