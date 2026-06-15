package at.pegelhub.lib.model;

import java.time.Instant;
import java.util.UUID;

/**
 * The model class used to send and receive {@code Measurement} objects.
 */
public class Measurement {
    private UUID timeSeriesId;
    private Instant observedAt;
    private Instant receivedAt;
    private Double value;
    private UUID submittedByConnectorId;

    public Measurement() {
    }

    public Measurement(UUID timeSeriesId, Instant observedAt, double value) {
        this(timeSeriesId, observedAt, null, value, null);
    }

    public Measurement(UUID timeSeriesId, Instant observedAt, Instant receivedAt, double value, UUID submittedByConnectorId) {
        this.timeSeriesId = timeSeriesId;
        this.observedAt = observedAt;
        this.receivedAt = receivedAt;
        this.value = value;
        this.submittedByConnectorId = submittedByConnectorId;
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

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public UUID getSubmittedByConnectorId() {
        return submittedByConnectorId;
    }

    public void setSubmittedByConnectorId(UUID submittedByConnectorId) {
        this.submittedByConnectorId = submittedByConnectorId;
    }

    @Override
    public String toString() {
        return "Measurement{" +
                "\n  timeSeriesId=" + timeSeriesId +
                ",\n  observedAt=" + observedAt +
                ",\n  receivedAt=" + receivedAt +
                ",\n  value=" + value +
                ",\n  submittedByConnectorId=" + submittedByConnectorId +
                "\n}";
    }
}
