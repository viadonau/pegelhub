package at.pegelhub.measurement.api.read.output;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "openapi.measurement.measurement-list-response.lean-envelope-of-raw-measurements-for-one")
public record MeasurementListResponse(
        @Schema(description = "openapi.measurement.measurement-list-response.time-series-whose-measurements-are-returned")
        UUID timeSeriesId,
        @Schema(description = "openapi.measurement.measurement-bucket-list-response.resolved-query-window")
        MeasurementWindowResponse window,
        @Schema(description = "openapi.measurement.measurement-list-response.sort-order-used-for-the-returned-measurements")
        MeasurementSortOrder order,
        @Schema(description = "openapi.measurement.measurement-list-response.requested-maximum-number-of-raw-points", example = "1000")
        int limit,
        @Schema(description = "openapi.measurement.measurement-list-response.whether-more-points-exist-in-the-requested", example = "false")
        boolean truncated,
        @Schema(description = "openapi.measurement.measurement-list-response.raw-measurement-points")
        List<MeasurementPointResponse> measurements) {
}
