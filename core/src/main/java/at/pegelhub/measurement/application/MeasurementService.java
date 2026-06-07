package at.pegelhub.measurement.application;

import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.measurement.domain.WriteMeasurements;
import at.pegelhub.timeseries.domain.TimeSeriesId;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for all {@code Measurement}s.
 */
public interface MeasurementService {

    /**
     * Saves multiple measurements.
     *
     * @param measurements to save.
     */
    void writeMeasurements(WriteMeasurements measurements);

    /**
     * Queries a measurement.
     *
     * @param range in which the returned values reside.
     * @return the measurements in that range.
     */
    List<Measurement> getByRange(String range);

    List<Measurement> getByTimeSeriesAndRange(TimeSeriesId timeSeriesId, String range);

    Measurement getLatestByTimeSeries(TimeSeriesId timeSeriesId);

    Measurement getAverageByTimeSeriesAndRange(TimeSeriesId timeSeriesId, String range);

    /**
     * Queries the last measurement.
     *
     * @param uuid of the measurement.
     * @return the last measurement with that uuid.
     */
    Measurement getLastData(UUID uuid);

    Instant getSystemTime();
}
