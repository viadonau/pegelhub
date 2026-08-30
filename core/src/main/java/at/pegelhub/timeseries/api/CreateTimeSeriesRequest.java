package at.pegelhub.timeseries.api;

import at.pegelhub.shared.metadata.MetadataStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "openapi.timeseries.create-time-series-request.request-to-create-observed-time-series-metadata")
public record CreateTimeSeriesRequest(
        @Schema(description = "openapi.timeseries.create-time-series-request.measuring-point-that-owns-this-time-series", format = "uuid", example = "58a21780-aa2f-4e1f-ae7e-5c48fd3f62dd")
        @NotNull UUID measuringPointId,
        @Schema(description = "openapi.timeseries.create-time-series-request.observed-property-code-for-example-water-level", example = "water-level")
        @NotBlank String observedProperty,
        @Schema(description = "openapi.timeseries.create-time-series-request.initial-status-defaults-to-active", defaultValue = "active")
        MetadataStatus status,
        @Schema(description = "openapi.timeseries.source-assignment-request.optional-source-assignment", nullable = true)
        @Valid SourceAssignmentRequest sourceAssignment) { }
