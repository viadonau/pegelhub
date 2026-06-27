package at.pegelhub.measurement.api.read.output;

import com.fasterxml.jackson.annotation.JsonValue;

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
