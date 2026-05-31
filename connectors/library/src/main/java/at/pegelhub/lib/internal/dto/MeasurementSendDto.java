package at.pegelhub.lib.internal.dto;

import java.time.Instant;
import java.util.Map;

public record MeasurementSendDto(Instant timestamp, Map<String, Double> fields, Map<String, String> infos) {}
