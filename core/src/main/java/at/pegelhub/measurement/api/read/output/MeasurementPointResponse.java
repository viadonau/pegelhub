package at.pegelhub.measurement.api.read.output;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "openapi.measurement.measurement-point-response.one-raw-measurement-point")
public record MeasurementPointResponse(
        @Schema(description = "openapi.measurement.measurement-point-response.time-at-which-the-value-was-observed", example = "2026-06-17T12:00:00Z")
        Instant observedAt,
        @Schema(description = "openapi.measurement.measurement-point-response.observed-numeric-value", example = "2.73")
        double value) {
}
