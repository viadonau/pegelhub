package at.pegelhub.quality.domain;

import java.time.Instant;
import java.util.UUID;

public record QualityProfile(
        UUID id,
        ProfileConfig configuration,
        Instant nextRunAt,
        UUID currentRunId,
        boolean runRequested) { }
