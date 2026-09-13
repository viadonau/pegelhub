package at.pegelhub.lib.config;

import java.util.Objects;

public record LoadedMapping<T>(
        String fileName,
        T value
) {
    public LoadedMapping {
        fileName = ConfigValidation.requireText(fileName, "fileName");
        Objects.requireNonNull(value, "value");
    }
}
