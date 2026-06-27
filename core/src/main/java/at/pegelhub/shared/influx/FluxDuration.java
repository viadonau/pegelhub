package at.pegelhub.shared.influx;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validated Flux duration literal used for relative ranges.
 */
public record FluxDuration(String value) {

    private static final Pattern DURATION = Pattern.compile("(?:[1-9]\\d*(?:ns|us|ms|s|m|h|d|w|mo|y))+");
    private static final Pattern DURATION_PART = Pattern.compile("([1-9]\\d*)(ns|us|ms|s|m|h|d|w|mo|y)");

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

    public static FluxDuration from(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("duration must be positive");
        }
        return new FluxDuration(duration.toNanos() + "ns");
    }

    public Duration toDuration() {
        Matcher matcher = DURATION_PART.matcher(value);
        Duration result = Duration.ZERO;
        int matchedUntil = 0;
        while (matcher.find()) {
            if (matcher.start() != matchedUntil) {
                throw new IllegalArgumentException("Invalid Flux duration: " + value);
            }
            long amount = Long.parseLong(matcher.group(1));
            result = result.plus(toDuration(amount, matcher.group(2)));
            matchedUntil = matcher.end();
        }
        if (matchedUntil != value.length() || result.isZero() || result.isNegative()) {
            throw new IllegalArgumentException("Invalid Flux duration: " + value);
        }
        return result;
    }

    private static Duration toDuration(long amount, String unit) {
        return switch (unit) {
            case "ns" -> Duration.ofNanos(amount);
            case "us" -> Duration.ofNanos(Math.multiplyExact(amount, 1_000));
            case "ms" -> Duration.ofMillis(amount);
            case "s" -> Duration.ofSeconds(amount);
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            case "d" -> Duration.ofDays(amount);
            case "w" -> Duration.ofDays(Math.multiplyExact(amount, 7));
            case "mo" -> Duration.ofDays(Math.multiplyExact(amount, 30));
            case "y" -> Duration.ofDays(Math.multiplyExact(amount, 365));
            default -> throw new IllegalArgumentException("Invalid duration unit: " + unit);
        };
    }

    @Override
    public String toString() {
        return value;
    }
}
