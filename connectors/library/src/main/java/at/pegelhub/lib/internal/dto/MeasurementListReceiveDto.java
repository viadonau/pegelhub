package at.pegelhub.lib.internal.dto;

import at.pegelhub.lib.model.Measurement;

import java.util.List;
import java.util.UUID;

public record MeasurementListReceiveDto(
        UUID timeSeriesId,
        List<MeasurementReceiveDto> measurements) {

    public List<Measurement> toMeasurements() {
        return measurements == null
                ? List.of()
                : measurements.stream()
                .map(measurement -> measurement.toMeasurement(timeSeriesId))
                .toList();
    }
}
