package at.pegelhub.shared.influx;

import java.util.regex.Pattern;

/**
 * Validated Flux duration literal used for relative ranges.
 */
public record FluxDuration(String value) {

    private static final Pattern DURATION = Pattern.compile("(?:[1-9]\\d*(?:ns|us|ms|s|m|h|d|w|mo|y))+");

    public FluxDuration {
        if (value == null) {
            throw new IllegalArgumentException("Flux duration must not be null");
        }
        value = value.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Flux duration must not be empty");
        }
        if (!DURATION.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid Flux duration: " + value);
        }
    }

    public String negativeLiteral() {
        return "-" + value;
    }

    @Override
    public String toString() {
        return value;
    }
}
