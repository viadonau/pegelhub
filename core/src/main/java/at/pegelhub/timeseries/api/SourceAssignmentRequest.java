package at.pegelhub.timeseries.api;

import at.pegelhub.timeseries.domain.SourceRepresentation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "openapi.timeseries.source-assignment-request.source-assignment")
public record SourceAssignmentRequest(
        @Schema(description = "openapi.timeseries.source-assignment-request.source-connector-identifier", format = "uuid", example = "a93fdc3d-b71f-44ce-a826-fe1dc1f1f357")
        @NotNull UUID connectorId,
        @Schema(description = "openapi.timeseries.source-assignment-request.source-value-representation")
        @NotNull SourceRepresentation representation) { }
