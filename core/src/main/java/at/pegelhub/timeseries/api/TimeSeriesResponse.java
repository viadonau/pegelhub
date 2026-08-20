package at.pegelhub.timeseries.api;

import at.pegelhub.shared.metadata.MetadataStatus;
import at.pegelhub.timeseries.domain.SourceRepresentation;

import java.util.UUID;

public record TimeSeriesResponse(
        UUID id,
        UUID measuringPointId,
        String observedProperty,
        String unit,
        MetadataStatus status,
        SourceAssignmentResponse sourceAssignment) {
    public record SourceAssignmentResponse(UUID connectorId, SourceRepresentation representation) { }
}
