package at.pegelhub.measurement.api.read.output;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Chart-ready average buckets for one time series.")
public record MeasurementBucketListResponse(
        @Schema(description = "Time series whose buckets are returned.")
        UUID timeSeriesId,
        @Schema(description = "Resolved query window.")
        MeasurementWindowResponse window,
        @Schema(description = "Bucket resolution and aggregation metadata.")
        MeasurementResolutionResponse resolution,
        @Schema(description = "Average measurement buckets.")
        List<MeasurementBucketPointResponse> points) {
}
