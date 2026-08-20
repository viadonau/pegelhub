package at.pegelhub.timeseries.domain;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.shared.metadata.MetadataStatus;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record TimeSeries(
        TimeSeriesId id,
        MeasuringPointId measuringPointId,
        ObservedPropertyCode observedProperty,
        MetadataStatus status,
        SourceAssignment sourceAssignment) {

    public TimeSeries {
        requireNonNull(id);
        requireNonNull(measuringPointId);
        requireNonNull(observedProperty);
        status = status == null ? MetadataStatus.ACTIVE : status;
        if (ObservedPropertyCatalog.find(observedProperty.value()).isEmpty()) {
            throw new IllegalArgumentException("Unknown observed property: " + observedProperty.value());
        }
        if (sourceAssignment != null
                && !ObservedPropertyCatalog.allows(observedProperty.value(), sourceAssignment.representation())) {
            throw new IllegalArgumentException(
                    "Source representation is not allowed for observed property " + observedProperty.value());
        }
    }

    public static TimeSeries create(
            MeasuringPointId measuringPointId,
            ObservedPropertyCode observedProperty,
            MetadataStatus status,
            SourceAssignment sourceAssignment) {
        return new TimeSeries(
                new TimeSeriesId(UUID.randomUUID()),
                measuringPointId,
                observedProperty,
                status,
                sourceAssignment);
    }

    public TimeSeries update(MetadataStatus status, SourceAssignment sourceAssignment) {
        return new TimeSeries(id, measuringPointId, observedProperty, status, sourceAssignment);
    }

    public String unit() {
        return observedProperty.definition().canonicalUnit();
    }

    public ConnectorId sourceConnectorId() {
        return sourceAssignment == null ? null : sourceAssignment.connectorId();
    }

    public SourceRepresentation sourceRepresentation() {
        return sourceAssignment == null ? null : sourceAssignment.representation();
    }
}
