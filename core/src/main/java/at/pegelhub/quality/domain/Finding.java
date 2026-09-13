package at.pegelhub.quality.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record Finding(
        Rule rule,
        Severity severity,
        List<UUID> timeSeriesIds,
        Instant observedAt,
        String message,
        Map<String, Double> evidence) {

    public enum Rule { RANGE, JUMP, FROZEN, DEVIATION, DATA }

    public enum Severity { WARNING, FAILED }

    public Finding {
        timeSeriesIds = List.copyOf(timeSeriesIds);
        evidence = Map.copyOf(evidence);
    }
}
