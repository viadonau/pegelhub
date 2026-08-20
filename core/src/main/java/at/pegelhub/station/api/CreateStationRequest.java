package at.pegelhub.station.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateStationRequest(@NotNull UUID ownerId, @NotBlank String name, @NotBlank String waterBody) { }
