package at.pegelhub.measurement.application;

import at.pegelhub.connector.domain.ConnectorId;

import java.time.Instant;

import static java.util.Objects.requireNonNull;

public record MeasurementPageRow(
        Instant observedAt,
        double value,
        ConnectorId submittedByConnectorId) {

    public MeasurementPageRow {
        requireNonNull(observedAt);
        requireNonNull(submittedByConnectorId);
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("value must be finite");
        }
    }
}
