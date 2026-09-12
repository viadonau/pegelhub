package at.pegelhub.timeseries.api;

import at.pegelhub.timeseries.domain.MeasurementRepresentation;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SourceAssignmentRequest(@NotNull UUID connectorId, @NotNull MeasurementRepresentation representation) { }
