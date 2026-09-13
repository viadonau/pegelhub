package at.pegelhub.lib.internal.dto;

import at.pegelhub.lib.model.Measurement;
import at.pegelhub.lib.model.MeasurementRepresentation;

import java.util.List;
import java.util.UUID;

public record MeasurementListReceiveDto(
        UUID timeSeriesId,
        boolean truncated,
        List<MeasurementReceiveDto> measurements,
        String representation,
        String unit) {

    /**
     * Checks that the response belongs to the requested series and uses the requested representation.
     * Run this check even if there are no rows, or if a truncated page will be discarded.
     */
    public void requireMatches(UUID expectedTimeSeriesId, MeasurementRepresentation requested) {
        requested.requireResponse(representation, unit);
        if (!expectedTimeSeriesId.equals(timeSeriesId)) {
            throw new IllegalStateException(
                    "Core returned measurements for " + timeSeriesId + " instead of " + expectedTimeSeriesId);
        }
    }

    /**
     * Builds measurements using the response's series ID, without converting values.
     * Call {@link #requireMatches(UUID, MeasurementRepresentation)} first to check the response against the request.
     */
    public List<Measurement> toMeasurements() {
        return measurements == null
                ? List.of()
                : measurements.stream()
                        .map(measurement -> measurement.toMeasurement(timeSeriesId))
                        .toList();
    }
}
