package at.pegelhub.measurement.api.read.output;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "openapi.measurement.measurement-bucket-list-response.chart-ready-average-buckets-for-one-time")
public record MeasurementBucketListResponse(
        @Schema(description = "openapi.measurement.measurement-bucket-list-response.time-series-whose-buckets-are-returned")
        UUID timeSeriesId,
        @Schema(description = "openapi.measurement.measurement-bucket-list-response.resolved-query-window")
        MeasurementWindowResponse window,
        @Schema(description = "openapi.measurement.measurement-bucket-list-response.bucket-resolution-and-aggregation-metadata")
        MeasurementResolutionResponse resolution,
        @Schema(description = "openapi.measurement.measurement-bucket-list-response.average-measurement-buckets")
        List<MeasurementBucketPointResponse> points) {
}
