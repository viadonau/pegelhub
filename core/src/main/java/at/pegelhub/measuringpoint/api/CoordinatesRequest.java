package at.pegelhub.measuringpoint.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "openapi.measuringpoint.coordinates-request.geographic-coordinates")
public record CoordinatesRequest(
        @Schema(description = "openapi.measuringpoint.coordinates-request.latitude", minimum = "-90", maximum = "90", example = "48.25")
        @NotNull BigDecimal latitude,
        @Schema(description = "openapi.measuringpoint.coordinates-request.longitude", minimum = "-180", maximum = "180", example = "16.39")
        @NotNull BigDecimal longitude) { }
