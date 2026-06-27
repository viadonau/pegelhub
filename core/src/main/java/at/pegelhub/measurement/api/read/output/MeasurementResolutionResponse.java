package at.pegelhub.measurement.api.read.output;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resolution metadata for chart-ready buckets.")
public record MeasurementResolutionResponse(
        @Schema(description = "Resolved bucket size.", example = "5m")
        String bucket,
        @Schema(description = "Aggregation function used for bucket values.", example = "average")
        MeasurementAggregation aggregation,
        @Schema(description = "Target point count used for automatic resolution. Null for an explicit bucket.", example = "500")
        Integer maxPoints) {
}
