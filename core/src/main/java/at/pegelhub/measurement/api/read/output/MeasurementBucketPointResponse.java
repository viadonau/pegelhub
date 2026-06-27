package at.pegelhub.measurement.api.read.output;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "One aggregated measurement bucket.")
public record MeasurementBucketPointResponse(
        @Schema(description = "Inclusive bucket start.", example = "2026-06-17T12:00:00Z")
        Instant from,
        @Schema(description = "Exclusive bucket end.", example = "2026-06-17T12:05:00Z")
        Instant to,
        @Schema(description = "Average value for the bucket.", example = "2.73")
        double value,
        @Schema(description = "Number of raw samples included in the bucket.", example = "12")
        long sampleCount) {
}
