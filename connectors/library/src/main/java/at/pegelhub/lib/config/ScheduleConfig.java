package at.pegelhub.lib.config;

import java.time.Duration;

public record ScheduleConfig(String delay) {
    public ScheduleConfig {
        if (delay == null || delay.isBlank()) {
            throw new IllegalArgumentException("schedule.delay must be configured");
        }
        delay = delay.trim();
    }

    public Duration interval() {
        String amount = delay.substring(0, delay.length() - 1);
        if (amount.isBlank() || !amount.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("schedule.delay must use a positive number followed by s, m, or h");
        }
        long value = Long.parseLong(amount);
        if (value <= 0) {
            throw new IllegalArgumentException("schedule.delay must be positive");
        }
        return switch (Character.toLowerCase(delay.charAt(delay.length() - 1))) {
            case 's' -> Duration.ofSeconds(value);
            case 'm' -> Duration.ofMinutes(value);
            case 'h' -> Duration.ofHours(value);
            default -> throw new IllegalArgumentException("schedule.delay must end with s, m, or h");
        };
    }
}
