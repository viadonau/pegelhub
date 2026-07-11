package at.pegelhub.lib.runtime;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

record ConnectorRuntimeConfig(Path configDir) {
    public static final String DEFAULT_CONFIG_DIR = "/app/config";

    public ConnectorRuntimeConfig {
        Objects.requireNonNull(configDir, "configDir");
    }

    public static ConnectorRuntimeConfig fromArgs(String[] args) {
        return new ConnectorRuntimeConfig(Path.of(args.length > 0 ? args[0] : DEFAULT_CONFIG_DIR));
    }

    public Path resolve(String fileName) {
        return configDir.resolve(fileName);
    }

    public static Duration parseDuration(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Duration must be configured");
        }

        String trimmed = value.trim();
        String amount = trimmed.substring(0, trimmed.length() - 1);
        char unit = Character.toLowerCase(trimmed.charAt(trimmed.length() - 1));
        return switch (unit) {
            case 'h' -> Duration.ofHours(Long.parseLong(amount));
            case 'm' -> Duration.ofMinutes(Long.parseLong(amount));
            case 's' -> Duration.ofSeconds(Long.parseLong(amount));
            default -> throw new IllegalArgumentException("Unknown duration unit: " + unit);
        };
    }

}
