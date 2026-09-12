package at.pegelhub.measurement.application;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * These values have already been converted to the query's representation.
 * The unit is included even when the list is empty.
 */
public record MeasurementList(
        MeasurementListQuery query,
        boolean truncated,
        List<MeasurementReadRow> measurements,
        String unit) {

    public MeasurementList {
        requireNonNull(query);
        requireNonNull(unit);
        measurements = List.copyOf(requireNonNull(measurements));
    }
}
