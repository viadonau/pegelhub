package at.pegelhub.timeseries.domain;

import at.pegelhub.connector.domain.ConnectorId;

import static java.util.Objects.requireNonNull;

/**
 * Identifies the connector that supplies a time series and the representation it sends.
 * Core uses the current setting for every write, including retries still buffered by the connector.
 * Changing it does not change stored measurements or the default representation returned by reads.
 */
public record SourceAssignment(
        ConnectorId connectorId,
        MeasurementRepresentation representation) {

    public SourceAssignment {
        requireNonNull(connectorId);
        requireNonNull(representation);
    }
}
