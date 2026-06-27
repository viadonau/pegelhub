package at.pegelhub.measurement.api.read.output;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Lean envelope of raw measurements for one time series.")
public record MeasurementListResponse(
        @Schema(description = "Time series whose measurements are returned.")
        UUID timeSeriesId,
        @Schema(description = "Resolved query window.")
        MeasurementWindowResponse window,
        @Schema(description = "Sort order used for the returned measurements.")
        MeasurementSortOrder order,
        @Schema(description = "Requested maximum number of raw points.", example = "1000")
        int limit,
        @Schema(description = "Whether more points exist after the returned measurements.", example = "false")
        boolean truncated,
        @Schema(description = "Fixed query parameters for the next page. Null when truncated is false.", nullable = true)
        MeasurementNextPageResponse next,
        @Schema(description = "Raw measurement points.")
        List<MeasurementPointResponse> measurements) {
}
