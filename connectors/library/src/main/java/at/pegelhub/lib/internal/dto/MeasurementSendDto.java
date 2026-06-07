package at.pegelhub.lib.internal.dto;

import java.time.Instant;
import java.util.UUID;

public record MeasurementSendDto(UUID timeSeriesId, Instant observedAt, Double value) {}
