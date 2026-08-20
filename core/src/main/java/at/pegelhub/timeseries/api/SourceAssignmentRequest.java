package at.pegelhub.timeseries.api;

import at.pegelhub.timeseries.domain.SourceRepresentation;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SourceAssignmentRequest(@NotNull UUID connectorId, @NotNull SourceRepresentation representation) { }
