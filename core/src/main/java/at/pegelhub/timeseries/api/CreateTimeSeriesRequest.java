package at.pegelhub.timeseries.api;

import at.pegelhub.shared.metadata.MetadataStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateTimeSeriesRequest(
        @NotNull UUID measuringPointId,
        @NotBlank String observedProperty,
        MetadataStatus status,
        @Valid SourceAssignmentRequest sourceAssignment) { }
