package at.pegelhub.measuringpoint.api;

import at.pegelhub.measuringpoint.domain.BankSide;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

import java.math.BigDecimal;

@Schema(description = "openapi.measuringpoint.position-request.physical-position")
public record PositionRequest(
        @Schema(description = "openapi.measuringpoint.position-request.optional-river-kilometer", minimum = "0", example = "1933.2", nullable = true)
        BigDecimal riverKilometer,
        @Schema(description = "openapi.measuringpoint.position-request.optional-bank-side", nullable = true)
        BankSide bank,
        @Schema(description = "openapi.measuringpoint.coordinates-request.optional-geographic-coordinates", nullable = true)
        @Valid CoordinatesRequest coordinates) { }
