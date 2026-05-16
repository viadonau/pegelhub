package at.pegelhub.measurement.domain;

import java.util.List;

import static at.pegelhub.shared.validation.Validations.requireNotEmpty;

/**
 * Domain class to write multiple measurements.
 */
public record WriteMeasurements(List<WriteMeasurement> measurements) {

    public WriteMeasurements {
        requireNotEmpty(measurements);
    }
}
