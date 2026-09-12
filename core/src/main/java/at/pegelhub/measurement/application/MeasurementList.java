package at.pegelhub.measurement.application;

import java.util.List;

import static java.util.Objects.requireNonNull;

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
