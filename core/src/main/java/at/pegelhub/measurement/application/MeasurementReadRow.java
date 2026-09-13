package at.pegelhub.measurement.application;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measurement.domain.InternalProducerId;

import java.time.Instant;

import static at.pegelhub.measurement.domain.MeasurementValues.requireFinite;
import static java.util.Objects.requireNonNull;

public record MeasurementReadRow(
        Instant observedAt,
        double value,
        ConnectorId submittedByConnectorId,
        InternalProducerId submittedByInternalProducerId) {

    public MeasurementReadRow(Instant observedAt, double value, ConnectorId submittedByConnectorId) {
        this(observedAt, value, requireNonNull(submittedByConnectorId), null);
    }

    public MeasurementReadRow {
        requireNonNull(observedAt);

        if ((submittedByConnectorId == null) == (submittedByInternalProducerId == null)) {
            throw new IllegalArgumentException("Exactly one measurement origin is required");
        }

        value = requireFinite(value);
    }
}
