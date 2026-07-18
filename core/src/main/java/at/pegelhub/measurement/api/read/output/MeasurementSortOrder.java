package at.pegelhub.measurement.api.read.output;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Sort order for raw measurement reads.", enumAsRef = true)
public enum MeasurementSortOrder {
    ASC("asc"),
    DESC("desc");

    private final String value;

    MeasurementSortOrder(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
