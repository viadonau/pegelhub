package at.pegelhub.measurement.api.read.output;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Parameters required to retrieve the next stable raw measurement page.")
public record MeasurementNextPageResponse(
        @Schema(description = "Inclusive fixed start of the resolved page window.")
        Instant from,
        @Schema(description = "Exclusive fixed end of the resolved page window.")
        Instant to,
        @Schema(description = "Sort order to reuse.")
        MeasurementSortOrder order,
        @Schema(description = "Page size to reuse.")
        int limit,
        @Schema(description = "Opaque stable cursor for the next page.")
        String cursor) {
}
