package at.pegelhub.quality.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable, normalized settings shared by API edits, YAML import and run snapshots.
 * Local shape/limit validation happens here; catalog compatibility, ownership and destination
 * existence require application-level validation within the profile edit transaction.
 */
public record ProfileConfig(
        String name,
        boolean enabled,
        List<UUID> sources,
        QualityRules rules,
        Integer lookbackSeconds,
        Integer intervalSeconds,
        List<UUID> destinations,
        UUID output) {

    public ProfileConfig {
        if (name == null || name.isBlank() || name.length() > 200 || name.contains("\r") || name.contains("\n")) {
            throw new IllegalArgumentException("Invalid profile name");
        }

        if (sources == null || sources.isEmpty() || sources.size() > 100 || sources.stream().anyMatch(Objects::isNull)
                || new HashSet<>(sources).size() != sources.size()) {
            throw new IllegalArgumentException("Select 1 to 100 distinct sources");
        }
        sources = List.copyOf(sources);

        rules = rules == null ? new QualityRules(null, null, null, null) : rules;
        lookbackSeconds = lookbackSeconds == null ? 1200 : lookbackSeconds;
        intervalSeconds = intervalSeconds == null ? 60 : intervalSeconds;
        if (lookbackSeconds < 1 || lookbackSeconds > 604800 || intervalSeconds < 1 || intervalSeconds > 86400) {
            throw new IllegalArgumentException("Invalid lookback or interval");
        }
        if (rules.frozenSeconds() != null && rules.frozenSeconds() > lookbackSeconds) {
            throw new IllegalArgumentException("Frozen duration cannot exceed the lookback window");
        }

        destinations = destinations == null ? List.of() : List.copyOf(destinations);
        if (destinations.size() > 100 || new HashSet<>(destinations).size() != destinations.size()) {
            throw new IllegalArgumentException("Invalid destinations");
        }

        if (output != null && sources.contains(output)) {
            throw new IllegalArgumentException("Output cannot be an input");
        }
    }
}
