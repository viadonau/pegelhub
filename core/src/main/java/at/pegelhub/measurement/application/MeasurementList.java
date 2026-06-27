package at.pegelhub.measurement.application;

import java.util.List;

import static java.util.Objects.requireNonNull;

public record MeasurementList(
        MeasurementListQuery query,
        boolean truncated,
        MeasurementCursor nextCursor,
        List<MeasurementPageRow> measurements) {

    public MeasurementList {
        requireNonNull(query);
        measurements = List.copyOf(requireNonNull(measurements));
    }
}
