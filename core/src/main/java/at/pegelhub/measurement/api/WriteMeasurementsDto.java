package at.pegelhub.measurement.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

import static at.pegelhub.shared.validation.Validations.requireNotEmpty;

/**
 * Dto to write multiple measurements.
 */
public record WriteMeasurementsDto(@NotEmpty List<@Valid WriteMeasurementDto> measurements) {

    public WriteMeasurementsDto {
        requireNotEmpty(measurements);
    }
}
