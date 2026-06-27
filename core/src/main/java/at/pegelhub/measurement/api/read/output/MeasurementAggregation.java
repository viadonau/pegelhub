package at.pegelhub.measurement.api.read.output;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MeasurementAggregation {
    AVERAGE("average");

    private final String value;

    MeasurementAggregation(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
