package at.pegelhub.lib;

import at.pegelhub.lib.model.Measurement;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Duration;

/**
 * Client used by connectors to exchange measurements with PegelHub Core.
 */
public interface PegelHubClient extends AutoCloseable {
    Collection<Measurement> getMeasurementsOfTimeSeries(UUID timeSeriesId, Duration lookback);

    Optional<Measurement> getLatestMeasurementOfTimeSeries(UUID timeSeriesId);

    void sendMeasurements(List<Measurement> measurements);
}
