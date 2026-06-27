package at.pegelhub.measurement.api.write;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Schema(description = "One raw measurement point to write.")
public record WriteMeasurementRequest(
        @Schema(description = "Time series that receives this measurement.", example = "018f5f4c-8d4a-7b1a-9f7b-0f6f6f6f6f6f")
        @NotNull UUID timeSeriesId,
        @Schema(description = "Time at which the value was observed.", example = "2026-06-17T12:00:00Z")
        @NotNull Instant observedAt,
        @Schema(description = "Observed numeric value.", example = "2.73")
        @NotNull Double value) {

    public WriteMeasurementRequest {
        requireNonNull(timeSeriesId);
        requireNonNull(observedAt);
        requireNonNull(value);
    }
}
