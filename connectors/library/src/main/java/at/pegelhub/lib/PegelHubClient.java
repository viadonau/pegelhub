package at.pegelhub.lib;

import at.pegelhub.lib.model.Measurement;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Client used by connectors to exchange measurements with PegelHub Core.
 */
public interface PegelHubClient extends AutoCloseable {
    Collection<Measurement> getMeasurementsOfTimeSeries(UUID timeSeriesId, String timespan);

    Optional<Measurement> getLatestMeasurementOfTimeSeries(UUID timeSeriesId);

    void sendMeasurements(List<Measurement> measurements);
}
