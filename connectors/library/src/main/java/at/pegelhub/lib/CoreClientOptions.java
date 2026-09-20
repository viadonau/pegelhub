package at.pegelhub.lib;

import java.time.Duration;

/**
 * Per-client HTTP timeouts and opt-in validation for monitoring reads.
 * Strict latest responses require explicit completeness and at most one valid point;
 * synchronization window reads retain the existing connector behavior.
 */
public record CoreClientOptions(Duration connectTimeout, Duration responseTimeout, boolean strictLatestResponse) {
    public CoreClientOptions {
        if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()
                || responseTimeout == null || responseTimeout.isNegative() || responseTimeout.isZero()) {
            throw new IllegalArgumentException("Core client timeouts must be positive");
        }
    }

    public static CoreClientOptions connectorDefaults() {
        return new CoreClientOptions(Duration.ofSeconds(10), Duration.ofSeconds(30), false);
    }
}
