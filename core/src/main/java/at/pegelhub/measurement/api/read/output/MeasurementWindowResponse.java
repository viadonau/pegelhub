package at.pegelhub.measurement.api.read.output;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "openapi.measurement.measurement-window-response.resolved-time-window-used-by-a-measurement")
public record MeasurementWindowResponse(
        @Schema(description = "openapi.measurement.measurement-window-response.inclusive-window-start", example = "2026-06-17T00:00:00Z")
        Instant from,
        @Schema(description = "openapi.measurement.measurement-window-response.exclusive-window-end", example = "2026-06-18T00:00:00Z")
        Instant to,
        @Schema(description = "openapi.measurement.measurement-window-response.original-relative-duration-when-requested-with-last", example = "24h", nullable = true)
        String requested) {
}
