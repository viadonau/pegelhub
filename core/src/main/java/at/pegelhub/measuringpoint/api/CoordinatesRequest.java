package at.pegelhub.measuringpoint.api;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CoordinatesRequest(@NotNull BigDecimal latitude, @NotNull BigDecimal longitude) { }
