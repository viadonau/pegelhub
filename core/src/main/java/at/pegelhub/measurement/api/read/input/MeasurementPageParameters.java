package at.pegelhub.measurement.api.read.input;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Instant;

/**
 * HTTP query parameters for a page of raw Measurements.
 */
public record MeasurementPageParameters(
        @Schema(description = "Positive relative window such as 3h, 24h, 7d, or 30d. Provide either last or both from and to.")
        String last,
        @Schema(description = "Inclusive explicit window start. Required with to when last is omitted.", example = "2026-06-17T00:00:00Z")
        Instant from,
        @Schema(description = "Exclusive explicit window end. Required with from when last is omitted.", example = "2026-06-18T00:00:00Z")
        Instant to,
        @Schema(description = "Sort order by observed time.", allowableValues = {"asc", "desc"}, defaultValue = "asc")
        String order,
        @Schema(description = "Maximum raw points to return.", minimum = "1", maximum = "10000", defaultValue = "1000")
        @Min(1) @Max(10_000) Integer limit,
        @Schema(description = "Opaque page position returned by the previous response.")
        String cursor) {
}
