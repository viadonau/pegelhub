package at.pegelhub.measurement.api;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Request DTO for submitting one scalar Measurement.
 */
public record WriteMeasurementDto(
        @NotNull UUID timeSeriesId,
        @NotNull Instant observedAt,
        @NotNull Double value) {

    public WriteMeasurementDto {
        requireNonNull(timeSeriesId);
        requireNonNull(observedAt);
        requireNonNull(value);
    }
}
