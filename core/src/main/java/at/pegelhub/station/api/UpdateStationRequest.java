package at.pegelhub.station.api;

import at.pegelhub.shared.metadata.MetadataStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateStationRequest(@NotBlank String name, @NotBlank String waterBody, @NotNull MetadataStatus status) { }
