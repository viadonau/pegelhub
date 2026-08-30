package at.pegelhub.timeseries.api;

import at.pegelhub.shared.metadata.MetadataStatus;
import at.pegelhub.timeseries.domain.SourceRepresentation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "openapi.timeseries.time-series-response.observed-time-series-metadata")
public record TimeSeriesResponse(
        @Schema(description = "openapi.measurement.measurement-api.time-series-identifier", format = "uuid", example = "8ce8c5b6-f093-4d46-b770-7239cdfa3d76")
        UUID id,
        @Schema(description = "openapi.timeseries.create-time-series-request.measuring-point-that-owns-this-time-series", format = "uuid", example = "58a21780-aa2f-4e1f-ae7e-5c48fd3f62dd")
        UUID measuringPointId,
        @Schema(description = "openapi.timeseries.time-series-response.observed-property-code", example = "water-level")
        String observedProperty,
        @Schema(description = "openapi.timeseries.time-series-response.canonical-unit", example = "cm")
        String unit,
        @Schema(description = "openapi.shared.metadata-status")
        MetadataStatus status,
        @Schema(description = "openapi.timeseries.source-assignment-request.optional-source-assignment", nullable = true)
        SourceAssignmentResponse sourceAssignment) {

    @Schema(description = "openapi.timeseries.source-assignment-request.source-assignment")
    public record SourceAssignmentResponse(
            @Schema(description = "openapi.timeseries.source-assignment-request.source-connector-identifier", format = "uuid", example = "a93fdc3d-b71f-44ce-a826-fe1dc1f1f357")
            UUID connectorId,
            @Schema(description = "openapi.timeseries.source-assignment-request.source-value-representation")
            SourceRepresentation representation) { }
}
