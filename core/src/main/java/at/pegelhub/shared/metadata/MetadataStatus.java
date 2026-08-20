package at.pegelhub.shared.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Locale;

import static java.util.Objects.requireNonNull;

/** Lifecycle shared by operational metadata resources. */
@Schema(description = "active or inactive operational metadata", enumAsRef = true)
public enum MetadataStatus {
    ACTIVE("active"),
    INACTIVE("inactive");

    private final String value;

    MetadataStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static MetadataStatus from(String value) {
        requireNonNull(value, "status must not be null");
        for (MetadataStatus status : values()) {
            if (status.value.equals(value.trim().toLowerCase(Locale.ROOT))) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }
}
