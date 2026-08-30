package at.pegelhub.connector.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Locale;

import static java.util.Objects.requireNonNull;

@Schema(description = "openapi.connector.connector-type", enumAsRef = true)
public enum ConnectorType {
    FTP("ftp"),
    TSTP("tstp"),
    IEC("iec"),
    ICC("icc"),
    MA("ma"),
    OTHER("other");

    private final String value;

    ConnectorType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static ConnectorType from(String value) {
        requireNonNull(value, "type must not be null");
        for (ConnectorType type : values()) {
            if (type.value.equals(value.trim().toLowerCase(Locale.ROOT))) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown connector type: " + value);
    }
}
