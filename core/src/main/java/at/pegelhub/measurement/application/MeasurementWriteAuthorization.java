package at.pegelhub.measurement.application;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.timeseries.domain.SourceRepresentation;
import at.pegelhub.timeseries.domain.TimeSeriesId;

import java.math.BigDecimal;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/** Immutable source snapshot used to authorize and normalize one write batch. */
public record MeasurementWriteAuthorization(
        ConnectorId connectorId,
        Map<TimeSeriesId, Normalization> timeSeries) {

    public MeasurementWriteAuthorization {
        requireNonNull(connectorId);
        requireNonNull(timeSeries);
        timeSeries = Map.copyOf(timeSeries);
    }

    public Normalization forTimeSeries(TimeSeriesId id) {
        requireNonNull(id);
        Normalization normalization = timeSeries.get(id);
        if (normalization == null) {
            throw new IllegalArgumentException("TimeSeries is not authorized for this write batch: " + id.value());
        }
        return normalization;
    }

    public record Normalization(SourceRepresentation representation, BigDecimal gaugeZeroElevationMAboveAdria) {
        public Normalization {
            requireNonNull(representation);
            if (representation == SourceRepresentation.METRES_ABOVE_ADRIA
                    && gaugeZeroElevationMAboveAdria == null) {
                throw new IllegalArgumentException("Absolute water-level source requires gauge zero elevation");
            }
        }
    }
}
