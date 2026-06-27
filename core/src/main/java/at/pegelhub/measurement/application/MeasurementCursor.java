package at.pegelhub.measurement.application;

import at.pegelhub.connector.domain.ConnectorId;

import java.time.Instant;

import static java.util.Objects.requireNonNull;

public record MeasurementCursor(
        Instant observedAt,
        ConnectorId submittedByConnectorId) {

    public MeasurementCursor {
        requireNonNull(observedAt);
        requireNonNull(submittedByConnectorId);
    }
}
