package at.pegelhub.lib.internal.dto;

import at.pegelhub.lib.model.Measurement;

import java.time.Instant;
import java.util.UUID;

public record MeasurementReceiveDto(
        IdDto timeSeriesId,
        Instant observedAt,
        Instant receivedAt,
        Double value,
        IdDto submittedByConnectorId) {

    public Measurement toMeasurement() {
        Measurement measurement = new Measurement();
        measurement.setTimeSeriesId(valueOf(timeSeriesId));
        measurement.setObservedAt(observedAt);
        measurement.setReceivedAt(receivedAt);
        measurement.setValue(value);
        measurement.setSubmittedByConnectorId(valueOf(submittedByConnectorId));
        return measurement;
    }

    private static UUID valueOf(IdDto id) {
        return id == null ? null : id.value();
    }

    public record IdDto(UUID value) {
    }
}
