package at.pegelhub.measurement.api.read.input;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record MeasurementIntervalParameters(
        @Schema(description = "openapi.measurement.measurement-interval-parameters.from", example = "2026-06-17T00:00:00Z")
        Instant from,
        @Schema(description = "openapi.measurement.measurement-interval-parameters.to", example = "2026-06-18T00:00:00Z")
        Instant to,
        @Schema(description = "openapi.measurement.measurement-interval-parameters.interval", allowableValues = {"15m", "1h", "1d"})
        String interval,
        @Schema(description = "openapi.measurement.measurement-interval-parameters.time-basis",
                allowableValues = {"UTC", "+01:00"}, defaultValue = "UTC")
        String timeBasis,
        @Schema(description = "openapi.measurement.measurement-interval-parameters.closed-only",
                defaultValue = "true")
        Boolean closedOnly,
        @Schema(description = "openapi.measurement.read.representation", defaultValue = "canonical")
        String representation) {
}
