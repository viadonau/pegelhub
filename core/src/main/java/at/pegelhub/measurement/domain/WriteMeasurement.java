package at.pegelhub.measurement.domain;

import java.time.Instant;
import java.util.Map;

import static at.pegelhub.shared.validation.Validations.requireNotEmpty;
import static java.util.Objects.requireNonNull;

/**
 * Domain class to create a single measurement.
 */
public record WriteMeasurement(Instant timestamp, Map<String, Double> fields, Map<String, String> infos) {

    public WriteMeasurement {
        requireNonNull(timestamp);
        requireNotEmpty(fields);
        requireNonNull(infos);
    }
}
