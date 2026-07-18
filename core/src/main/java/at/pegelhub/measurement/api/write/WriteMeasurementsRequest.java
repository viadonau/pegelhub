package at.pegelhub.measurement.api.write;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

import static at.pegelhub.shared.validation.Validations.requireNotEmpty;

@Schema(description = "Batch of raw measurements to write.")
public record WriteMeasurementsRequest(
        @ArraySchema(
                minItems = 1,
                schema = @Schema(description = "One measurement to store."))
        @NotEmpty List<@Valid WriteMeasurementRequest> measurements) {

    public WriteMeasurementsRequest {
        requireNotEmpty(measurements);
    }
}
