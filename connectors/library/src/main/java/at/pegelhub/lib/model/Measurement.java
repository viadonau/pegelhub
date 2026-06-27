package at.pegelhub.lib.model;

import java.time.Instant;
import java.util.UUID;

/**
 * The model class used to send and receive {@code Measurement} objects.
 */
public class Measurement {
    private UUID timeSeriesId;
    private Instant observedAt;
    private Double value;

    public Measurement() {
    }

    public Measurement(UUID timeSeriesId, Instant observedAt, double value) {
        this.timeSeriesId = timeSeriesId;
        this.observedAt = observedAt;
        this.value = value;
    }

    public UUID getTimeSeriesId() {
        return timeSeriesId;
    }

    public void setTimeSeriesId(UUID timeSeriesId) {
        this.timeSeriesId = timeSeriesId;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(Instant observedAt) {
        this.observedAt = observedAt;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Measurement{" +
                "\n  timeSeriesId=" + timeSeriesId +
                ",\n  observedAt=" + observedAt +
                ",\n  value=" + value +
                "\n}";
    }
}
