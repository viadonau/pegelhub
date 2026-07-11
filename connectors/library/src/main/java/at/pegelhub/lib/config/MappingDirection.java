package at.pegelhub.lib.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum MappingDirection {
    EXTERNAL_TO_CORE("external-to-core"),
    CORE_TO_EXTERNAL("core-to-external");

    private final String value;

    MappingDirection(String value) {
        this.value = value;
    }

    @JsonCreator
    public static MappingDirection fromValue(String value) {
        return Arrays.stream(values())
                .filter(direction -> direction.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown mapping direction: " + value));
    }

    @JsonValue
    public String value() {
        return value;
    }
}
