package at.pegelhub.measurement.api.read.input;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Instant;

/**
 * HTTP query parameters for chart-ready Measurement buckets.
 */
public record MeasurementBucketParameters(
        @Schema(description = "openapi.measurement.measurement-bucket-parameters.positive-relative-window-such-as-3h-24h")
        String last,
        @Schema(description = "openapi.measurement.measurement-bucket-parameters.inclusive-explicit-window-start-required-with-to", example = "2026-06-17T00:00:00Z")
        Instant from,
        @Schema(description = "openapi.measurement.measurement-bucket-parameters.exclusive-explicit-window-end-required-with-from", example = "2026-06-18T00:00:00Z")
        Instant to,
        @Schema(description = "openapi.measurement.measurement-bucket-parameters.explicit-fixed-aggregation-width-mutually-exclusive-with", example = "5m")
        String bucket,
        @Schema(description = "openapi.measurement.measurement-bucket-parameters.target-maximum-points-for-automatic-bucket-resolution", minimum = "1", maximum = "10000", defaultValue = "500")
        @Min(1) @Max(10_000) Integer maxPoints) {
}
