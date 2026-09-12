package at.pegelhub.quality.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Historical configuration snapshot and advisory result. Output has an independent state because
 * an Influx write cannot commit atomically with findings in PostgreSQL. PASSED does not imply output was written.
 */
public record QualityRun(
        UUID id,
        UUID profileId,
        ProfileConfig configuration,
        State state,
        Instant startedAt,
        Instant completedAt,
        int findingCount,
        OutputState outputState,
        String error) {

    public enum State { RUNNING, PASSED, FINDINGS, INCOMPLETE, ERROR, INTERRUPTED }

    public enum OutputState { NOT_CONFIGURED, SUPPRESSED, PENDING, WRITTEN, FAILED, INTERRUPTED }
}
