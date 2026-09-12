package at.pegelhub.measurement.domain;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.timeseries.domain.TimeSeriesId;

import java.time.Instant;

import static at.pegelhub.measurement.domain.MeasurementValues.requireFinite;
import static java.util.Objects.requireNonNull;

/**
 * A persisted scalar observation with exactly one origin: connector or internal producer.
 * Observation time identifies the sample; receipt time is separate metadata and may change on a repeat write.
 */
public record Measurement(
        TimeSeriesId timeSeriesId,
        Instant observedAt,
        Instant receivedAt,
        double value,
        ConnectorId submittedByConnectorId,
        InternalProducerId submittedByInternalProducerId) {

    public Measurement(TimeSeriesId timeSeriesId, Instant observedAt, Instant receivedAt,
                       double value, ConnectorId submittedByConnectorId) {
        this(timeSeriesId, observedAt, receivedAt, value, requireNonNull(submittedByConnectorId), null);
    }

    public Measurement {
        requireNonNull(timeSeriesId);
        requireNonNull(observedAt);
        requireNonNull(receivedAt);

        if ((submittedByConnectorId == null) == (submittedByInternalProducerId == null)) {
            throw new IllegalArgumentException("Exactly one measurement origin is required");
        }

        value = requireFinite(value);
    }
}
