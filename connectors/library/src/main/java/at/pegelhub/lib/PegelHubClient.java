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

    /** Reads the complete time range in canonical storage units. */
    default Collection<Measurement> getMeasurementsOfTimeSeries(UUID timeSeriesId, Instant from, Instant to) {
        return getMeasurementsOfTimeSeries(timeSeriesId, from, to, MeasurementRepresentation.CANONICAL);
    }

    /**
     * Returns all measurements from {@code from} (inclusive) to {@code to} (exclusive), in the requested representation.
     * If part of the read fails, throw rather than return an incomplete list. Otherwise a connector
     * could skip the missing data on its next poll. The data may change between page requests.
     *
     * <p>Core must confirm the requested representation and return a unit, even when there are no measurements.
     * Fail the read if the metadata is missing or mismatched; never fall back to a different representation.
     * Core has already converted the values, so the caller must not convert them again.
     */
    Collection<Measurement> getMeasurementsOfTimeSeries(
            UUID timeSeriesId,
            Instant from,
            Instant to,
            MeasurementRepresentation representation);

    /** Returns the latest measurement in storage units, within the time range searched by the client. */
    default Optional<Measurement> getLatestMeasurementOfTimeSeries(UUID timeSeriesId) {
        return getLatestMeasurementOfTimeSeries(timeSeriesId, MeasurementRepresentation.CANONICAL);
    }

    /**
     * Returns the latest value in the requested representation, using the same response checks as
     * {@link #getMeasurementsOfTimeSeries(UUID, Instant, Instant, MeasurementRepresentation)}.
     * Returns an empty result when there are no measurements, but throws if a response check fails.
     */
    Optional<Measurement> getLatestMeasurementOfTimeSeries(
            UUID timeSeriesId,
            MeasurementRepresentation representation);

    /**
     * Sends values as provided. Core converts them using the input representation configured for each time series.
     */
    void sendMeasurements(List<Measurement> measurements);
}
