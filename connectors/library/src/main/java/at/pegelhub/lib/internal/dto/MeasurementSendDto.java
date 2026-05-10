package at.pegelhub.lib.internal.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record MeasurementSendDto(LocalDateTime timestamp, Map<String, Double> fields, Map<String, String> infos) {}
