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

    public void requireRepresentation(MeasurementRepresentation requested) {
        requested.requireResponse(representation, unit);
    }

    public List<Measurement> toMeasurements(UUID expectedTimeSeriesId) {
        if (!expectedTimeSeriesId.equals(timeSeriesId)) {
            throw new IllegalStateException(
                    "Core returned measurements for " + timeSeriesId + " instead of " + expectedTimeSeriesId);
        }
        return measurements == null
                ? List.of()
                : measurements.stream()
                        .map(measurement -> measurement.toMeasurement(timeSeriesId))
                        .toList();
    }
}
