package at.pegelhub.measuringpoint.api;

import at.pegelhub.measuringpoint.domain.BankSide;
import jakarta.validation.Valid;

import java.math.BigDecimal;

public record PositionRequest(BigDecimal riverKilometer, BankSide bank, @Valid CoordinatesRequest coordinates) { }
