package at.pegelhub.measurement.api.read.output;

import at.pegelhub.timeseries.domain.MeasurementRepresentation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "openapi.measurement.measurement-interval-list-response.description")
public record MeasurementIntervalListResponse(
        UUID timeSeriesId,
        Instant from,
        Instant to,
        String interval,
        String timeBasis,
        boolean closedOnly,
        MeasurementRepresentation representation,
        String unit,
        @Schema(description = "openapi.measurement.measurement-interval-list-response.method")
        String method,
        @Schema(description = "openapi.measurement.measurement-interval-list-response.computed-at")
        Instant computedAt,
        List<IntervalResponse> intervals) {

    public record IntervalResponse(
            Instant from,
            Instant to,
            Double mean,
            long observationCount,
            long supportedNanos,
            Instant lastContributingObservedAt,
            String windowStatus,
            String supportStatus) { }
}
