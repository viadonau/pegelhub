package at.pegelhub.measurement.application;

import at.pegelhub.connector.domain.ConnectorId;

import java.time.Instant;

import static at.pegelhub.measurement.domain.MeasurementValues.requireFinite;
import static java.util.Objects.requireNonNull;

public record MeasurementReadRow(
        Instant observedAt,
        double value,
        ConnectorId submittedByConnectorId) {

    public MeasurementReadRow {
        requireNonNull(observedAt);
        requireNonNull(submittedByConnectorId);
        value = requireFinite(value);
    }
}
