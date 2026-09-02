package at.pegelhub.timeseries.api;

import at.pegelhub.shared.metadata.MetadataStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Schema(description = "openapi.timeseries.update-time-series-request.request-to-replace-time-series-mapping-metadata")
public record UpdateTimeSeriesRequest(
        @Schema(description = "openapi.shared.metadata-status")
        @NotNull MetadataStatus status,
        @Schema(description = "openapi.timeseries.source-assignment-request.optional-source-assignment", nullable = true)
        @Valid SourceAssignmentRequest sourceAssignment) { }
