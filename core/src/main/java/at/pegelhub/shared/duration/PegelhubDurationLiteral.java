package at.pegelhub.shared.duration;

import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Duration format used by PegelHub APIs and Flux adapters.
 */
public record PegelhubDurationLiteral(String value) {

    private static final Pattern DURATION_PART = Pattern.compile("([1-9]\\d*)([smhdw])");
    private static final long SECONDS_PER_MINUTE = 60;
    private static final long SECONDS_PER_HOUR = 60 * SECONDS_PER_MINUTE;
    private static final long SECONDS_PER_DAY = 24 * SECONDS_PER_HOUR;

    public PegelhubDurationLiteral {
        if (value == null) {
            throw new IllegalArgumentException("PegelHub duration must not be null");
        }
        value = value.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("PegelHub duration must not be empty");
        }
        parse(value);
    }

    public static PegelhubDurationLiteral from(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("duration must be positive");
        }
        if (duration.getNano() != 0) {
            throw new IllegalArgumentException("duration must use whole seconds");
        }
        long seconds = duration.getSeconds();
        if (seconds % SECONDS_PER_DAY == 0) {
            return new PegelhubDurationLiteral(seconds / SECONDS_PER_DAY + "d");
        }
        if (seconds % SECONDS_PER_HOUR == 0) {
            return new PegelhubDurationLiteral(seconds / SECONDS_PER_HOUR + "h");
        }
        if (seconds % SECONDS_PER_MINUTE == 0) {
            return new PegelhubDurationLiteral(seconds / SECONDS_PER_MINUTE + "m");
        }
        return new PegelhubDurationLiteral(seconds + "s");
    }

    public Duration toDuration() {
        return parse(value);
    }

    private static Duration parse(String value) {
        Matcher matcher = DURATION_PART.matcher(value);
        Duration result = Duration.ZERO;
        int matchedUntil = 0;
        while (matcher.find()) {
            if (matcher.start() != matchedUntil) {
                throw invalid(value);
            }
            long amount = Long.parseLong(matcher.group(1));
            result = result.plus(toDuration(amount, matcher.group(2)));
            matchedUntil = matcher.end();
        }
        if (matchedUntil != value.length() || result.isZero() || result.isNegative()) {
            throw invalid(value);
        }
        return result;
    }

    private static Duration toDuration(long amount, String unit) {
        return switch (unit) {
            case "s" -> Duration.ofSeconds(amount);
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            case "d" -> Duration.ofDays(amount);
            case "w" -> Duration.ofDays(Math.multiplyExact(amount, 7));
            default -> throw new IllegalArgumentException("Invalid duration unit: " + unit);
        };
    }

    private static IllegalArgumentException invalid(String value) {
        return new IllegalArgumentException("Invalid PegelHub duration: " + value);
    }

    @Override
    public @NonNull String toString() {
        return value;
    }
}
