package at.pegelhub.lib.config;

public final class ConfigValidation {
    private ConfigValidation() {
    }

    public static String requireText(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(property + " must be configured");
        }
        return value;
    }

    public static int requirePositive(int value, String property) {
        if (value <= 0) {
            throw new IllegalArgumentException(property + " must be positive");
        }
        return value;
    }

    public static int requireTcpPort(int value, String property) {
        if (value < 1 || value > 65535) {
            throw new IllegalArgumentException(property + " must be between 1 and 65535");
        }
        return value;
    }
}
