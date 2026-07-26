package at.pegelhub.measurement.api.read.output;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "openapi.measurement.measurement-resolution-response.resolution-metadata-for-chart-ready-buckets")
public record MeasurementResolutionResponse(
        @Schema(description = "openapi.measurement.measurement-resolution-response.resolved-bucket-size", example = "5m")
        String bucket,
        @Schema(description = "openapi.measurement.measurement-resolution-response.aggregation-function-used-for-bucket-values", example = "average")
        MeasurementAggregation aggregation,
        @Schema(description = "openapi.measurement.measurement-resolution-response.target-point-count-used-for-automatic-resolution", example = "500")
        Integer maxPoints) {
}
