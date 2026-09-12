package at.pegelhub.lib;

import at.pegelhub.lib.model.Measurement;
import at.pegelhub.lib.model.MeasurementRepresentation;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Client used by connectors to exchange measurements with PegelHub Core.
 */
public interface PegelHubClient extends AutoCloseable {
    /** Returns all measurements in the half-open interval {@code [from, to)}. */
    Collection<Measurement> getMeasurementsOfTimeSeries(UUID timeSeriesId, Instant from, Instant to);

    Optional<Measurement> getLatestMeasurementOfTimeSeries(UUID timeSeriesId);

    /** Values use the requested representation, which Core must explicitly confirm. */
    default Collection<Measurement> getMeasurementsOfTimeSeries(
            UUID timeSeriesId,
            Instant from,
            Instant to,
            MeasurementRepresentation representation) {
        if (representation != MeasurementRepresentation.CANONICAL) {
            throw new UnsupportedOperationException("This Core client does not support represented reads");
        }
        return getMeasurementsOfTimeSeries(timeSeriesId, from, to);
    }

    default Optional<Measurement> getLatestMeasurementOfTimeSeries(
            UUID timeSeriesId,
            MeasurementRepresentation representation) {
        if (representation != MeasurementRepresentation.CANONICAL) {
            throw new UnsupportedOperationException("This Core client does not support represented reads");
        }
        return getLatestMeasurementOfTimeSeries(timeSeriesId);
    }

    void sendMeasurements(List<Measurement> measurements);
}
