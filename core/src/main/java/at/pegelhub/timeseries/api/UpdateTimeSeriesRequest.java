package at.pegelhub.timeseries.api;

import at.pegelhub.shared.metadata.MetadataStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UpdateTimeSeriesRequest(@NotNull MetadataStatus status, @Valid SourceAssignmentRequest sourceAssignment) { }
