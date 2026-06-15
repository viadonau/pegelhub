package at.pegelhub.lib;

import at.pegelhub.lib.model.Measurement;

import java.time.Instant;
import java.util.*;

/**
 * API spec for measurement communication.
 */
public interface MeasurementAPI {
    Collection<Measurement> getMeasurementsOfTimeSeries(UUID timeSeriesId, String timespan);

    Optional<Measurement> getLatestMeasurementOfTimeSeries(UUID timeSeriesId);

    void sendMeasurements(List<Measurement> meass);

    Instant getSystemTime();
}
