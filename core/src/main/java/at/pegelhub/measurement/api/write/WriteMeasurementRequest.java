package at.pegelhub.measurement.api.write;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Schema(description = "openapi.measurement.write-measurement-request.one-raw-measurement-point-to-write")
public record WriteMeasurementRequest(
        @Schema(description = "openapi.measurement.write-measurement-request.time-series-that-receives-this-measurement", format = "uuid", example = "018f5f4c-8d4a-7b1a-9f7b-0f6f6f6f6f6f")
        @NotNull UUID timeSeriesId,
        @Schema(description = "openapi.measurement.measurement-point-response.time-at-which-the-value-was-observed", type = "string", format = "date-time", example = "2026-06-17T12:00:00Z")
        @NotNull Instant observedAt,
        @Schema(description = "openapi.measurement.measurement-point-response.observed-numeric-value", example = "2.73")
        @NotNull Double value) {

    public WriteMeasurementRequest {
        requireNonNull(timeSeriesId);
        requireNonNull(observedAt);
        requireNonNull(value);
    }
}
