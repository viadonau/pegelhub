package at.pegelhub.lib.config;

import java.util.Objects;

public record LoadedMapping<T>(
        String fileName,
        T value
) {
    public LoadedMapping {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        Objects.requireNonNull(value, "value");
    }
}
