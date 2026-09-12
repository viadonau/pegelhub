package at.pegelhub.timeseries.domain;

import at.pegelhub.connector.domain.ConnectorId;

import static java.util.Objects.requireNonNull;

public record SourceAssignment(
        ConnectorId connectorId,
        MeasurementRepresentation representation) {

    public SourceAssignment {
        requireNonNull(connectorId);
        requireNonNull(representation);
    }
}
