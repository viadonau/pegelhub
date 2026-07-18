package at.pegelhub.lib.config;

import java.time.Duration;

public record PollingConfig(
        String interval
) {
    public PollingConfig {
        interval = ConfigValidation.requireText(interval, "polling.interval").trim();
    }

    public Duration duration() {
        String amount = interval.substring(0, interval.length() - 1);
        if (amount.isBlank() || !amount.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException(
                    "polling.interval must use a positive number followed by s, m, or h");
        }
        long value = Long.parseLong(amount);
        if (value <= 0) {
            throw new IllegalArgumentException("polling.interval must be positive");
        }
        return switch (Character.toLowerCase(interval.charAt(interval.length() - 1))) {
            case 's' -> Duration.ofSeconds(value);
            case 'm' -> Duration.ofMinutes(value);
            case 'h' -> Duration.ofHours(value);
            default -> throw new IllegalArgumentException("polling.interval must end with s, m, or h");
        };
    }
}
