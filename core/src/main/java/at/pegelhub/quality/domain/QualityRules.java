package at.pegelhub.quality.domain;

/** Optional checks: null disables a rule, while zero is a valid, enabled value threshold in the series' canonical unit. */
public record QualityRules(Range range, Double jumpThreshold, Long frozenSeconds, Double maxDeviation) {

    public QualityRules {
        threshold(jumpThreshold);
        threshold(maxDeviation);

        if (frozenSeconds != null && (frozenSeconds < 1 || frozenSeconds > 604800)) {
            throw new IllegalArgumentException("Frozen duration must be 1 to 604800 seconds");
        }
    }

    public record Range(double min, double max) {

        public Range {
            if (!Double.isFinite(min) || !Double.isFinite(max) || min > max) {
                throw new IllegalArgumentException("Invalid range");
            }
        }
    }

    private static void threshold(Double value) {
        if (value != null && (!Double.isFinite(value) || value < 0)) {
            throw new IllegalArgumentException("Threshold must be finite and nonnegative");
        }
    }
}
