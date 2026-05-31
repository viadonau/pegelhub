package at.pegelhub.measurement.api;

import java.time.Instant;
import java.util.Map;

import static at.pegelhub.shared.validation.Validations.requireNotEmpty;
import static java.util.Objects.requireNonNull;

/**
 * Dto to create a single measurement.
 */
public record WriteMeasurementDto(Instant timestamp,
                                  Map<String, Double> fields, Map<String, String> infos) {

    public WriteMeasurementDto {
        requireNonNull(timestamp);
        requireNotEmpty(fields);
        requireNonNull(infos);
    }
}
