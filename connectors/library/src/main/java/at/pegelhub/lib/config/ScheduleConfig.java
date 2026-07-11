package at.pegelhub.lib.config;

public record ScheduleConfig(String delay) {
    public ScheduleConfig {
        if (delay == null || delay.isBlank()) {
            throw new IllegalArgumentException("schedule.delay must be configured");
        }
        delay = delay.trim();
    }
}
