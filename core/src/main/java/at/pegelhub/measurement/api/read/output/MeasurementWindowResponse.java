package at.pegelhub.measurement.api.read.output;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Resolved time window used by a measurement query.")
public record MeasurementWindowResponse(
        @Schema(description = "Inclusive window start.", example = "2026-06-17T00:00:00Z")
        Instant from,
        @Schema(description = "Exclusive window end.", example = "2026-06-18T00:00:00Z")
        Instant to,
        @Schema(description = "Original relative duration when requested with last. Null for explicit from/to windows.", example = "24h", nullable = true)
        String requested) {
}
