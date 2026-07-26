package at.pegelhub.measurement.api.read.output;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "openapi.measurement.measurement-bucket-point-response.one-aggregated-measurement-bucket")
public record MeasurementBucketPointResponse(
        @Schema(description = "openapi.measurement.measurement-bucket-point-response.inclusive-bucket-start", example = "2026-06-17T12:00:00Z")
        Instant from,
        @Schema(description = "openapi.measurement.measurement-bucket-point-response.exclusive-bucket-end", example = "2026-06-17T12:05:00Z")
        Instant to,
        @Schema(description = "openapi.measurement.measurement-bucket-point-response.average-value-for-the-bucket", example = "2.73")
        double value,
        @Schema(description = "openapi.measurement.measurement-bucket-point-response.number-of-raw-samples-included-in-the", example = "12")
        long sampleCount) {
}
