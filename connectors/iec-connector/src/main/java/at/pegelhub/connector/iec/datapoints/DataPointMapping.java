package at.pegelhub.connector.iec.datapoints;

import at.pegelhub.lib.config.DirectedMapping;
import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.model.MeasurementRepresentation;

import java.util.Objects;
import java.util.UUID;

/**
 * Links an IEC IOA to a Core time series in one direction.
 *
 * <p>For Core-to-IEC mappings, {@code outputRepresentation} tells Core which representation to return.
 * The connector sends those values without converting them. If omitted, it uses canonical values.
 * For IEC-to-Core mappings, leave it canonical: the source assignment in Core defines the input representation.
 */
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
