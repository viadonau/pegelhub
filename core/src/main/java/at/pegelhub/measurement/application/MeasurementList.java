package at.pegelhub.measurement.application;

import java.util.List;

import static java.util.Objects.requireNonNull;

public record MeasurementList(
        MeasurementListQuery query,
        boolean truncated,
        List<MeasurementReadRow> measurements) {

    public MeasurementList {
        requireNonNull(query);
        measurements = List.copyOf(requireNonNull(measurements));
    }
}
