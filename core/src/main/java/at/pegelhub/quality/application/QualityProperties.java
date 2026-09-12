package at.pegelhub.quality.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Global execution budgets across a run's sources; enabling the worker does not enable individual profiles. */
@ConfigurationProperties("pegelhub.quality")
public record QualityProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("100000") int maximumPoints,
        @DefaultValue("120") int timeoutSeconds,
        @DefaultValue("90") int retentionDays) {

    public QualityProperties {
        if (maximumPoints < 1 || maximumPoints > 1_000_000
                || timeoutSeconds < 1 || timeoutSeconds > 3600 || retentionDays < 1) {
            throw new IllegalArgumentException("Invalid quality execution limits");
        }
    }
}
