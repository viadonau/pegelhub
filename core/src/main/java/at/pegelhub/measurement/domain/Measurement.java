package at.pegelhub.measurement.domain;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.timeseries.domain.TimeSeriesId;

import java.time.Instant;

import static at.pegelhub.measurement.domain.MeasurementValues.requireFinite;
import static java.util.Objects.requireNonNull;

/**
 * A persisted scalar observation for one TimeSeries.
 */
public record Measurement(
        TimeSeriesId timeSeriesId,
        Instant observedAt,
        Instant receivedAt,
        double value,
        ConnectorId submittedByConnectorId) {

    public Measurement {
        requireNonNull(timeSeriesId);
        requireNonNull(observedAt);
        requireNonNull(receivedAt);
        requireNonNull(submittedByConnectorId);
        value = requireFinite(value);
    }
}
