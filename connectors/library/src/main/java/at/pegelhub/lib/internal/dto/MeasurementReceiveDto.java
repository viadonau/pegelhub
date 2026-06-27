package at.pegelhub.lib.internal.dto;

import at.pegelhub.lib.model.Measurement;

import java.time.Instant;
import java.util.UUID;

public record MeasurementReceiveDto(
        Instant observedAt,
        Double value) {

    public Measurement toMeasurement(UUID timeSeriesId) {
        Measurement measurement = new Measurement();
        measurement.setTimeSeriesId(timeSeriesId);
        measurement.setObservedAt(observedAt);
        measurement.setValue(value);
        return measurement;
    }
}
