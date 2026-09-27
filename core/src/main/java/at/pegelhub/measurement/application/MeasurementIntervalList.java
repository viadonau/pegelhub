package at.pegelhub.measurement.application;

import java.time.Instant;
import java.util.List;

public record MeasurementIntervalList(
        MeasurementIntervalQuery query,
        Instant computedAt,
        String unit,
        List<MeasurementInterval> intervals) {
}
