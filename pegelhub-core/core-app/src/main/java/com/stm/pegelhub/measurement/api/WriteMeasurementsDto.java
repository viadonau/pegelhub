package com.stm.pegelhub.measurement.api;

import java.util.List;

import static com.stm.pegelhub.shared.validation.Validations.requireNotEmpty;

/**
 * Dto to write multiple measurements.
 */
public record WriteMeasurementsDto(List<WriteMeasurementDto> measurements) {

    public WriteMeasurementsDto {
        requireNotEmpty(measurements);
    }
}
