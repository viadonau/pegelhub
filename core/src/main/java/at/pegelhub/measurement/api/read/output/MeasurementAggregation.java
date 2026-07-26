package at.pegelhub.measurement.api.read.output;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "openapi.measurement.measurement-aggregation.aggregation-used-for-measurement-buckets", enumAsRef = true)
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
