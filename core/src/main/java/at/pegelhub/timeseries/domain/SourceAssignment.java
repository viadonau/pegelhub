package at.pegelhub.timeseries.domain;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measurement.domain.InternalProducerId;

import static java.util.Objects.requireNonNull;

/**
 * Exclusive write ownership, not an access role. Internal producers always write canonical values;
 * the two-argument constructor preserves the existing connector assignment contract.
 * Core uses the current connector representation for every write, including buffered retries.
 * Changing it does not change stored measurements or the default representation returned by reads.
 */
public record SourceAssignment(
        ConnectorId connectorId,
        MeasurementRepresentation representation,
        InternalProducerId internalProducerId) {

    public SourceAssignment(ConnectorId connectorId, MeasurementRepresentation representation) {
        this(requireNonNull(connectorId), representation, null);
    }

    public static SourceAssignment internal(InternalProducerId id) {
        return new SourceAssignment(null, MeasurementRepresentation.CANONICAL, requireNonNull(id));
    }

    public SourceAssignment {
        requireNonNull(representation);

        if ((connectorId == null) == (internalProducerId == null)) {
            throw new IllegalArgumentException("Exactly one source identity is required");
        }
        if (internalProducerId != null && representation != MeasurementRepresentation.CANONICAL) {
            throw new IllegalArgumentException("Internal producers write canonical values");
        }
    }
}
