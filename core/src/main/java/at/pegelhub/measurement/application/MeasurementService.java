package at.pegelhub.measurement.application;

import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.measurement.domain.MeasurementAverage;
import at.pegelhub.measurement.domain.WriteMeasurements;
import at.pegelhub.timeseries.domain.TimeSeriesId;

import java.time.Instant;
import java.util.List;

/**
 * Service for all {@code Measurement}s.
 */
public interface MeasurementService {

    void writeMeasurements(WriteMeasurements measurements);

    List<Measurement> getByTimeSeriesAndRange(TimeSeriesId timeSeriesId, String range);

    Measurement getLatestByTimeSeries(TimeSeriesId timeSeriesId);

    MeasurementAverage getAverageByTimeSeriesAndRange(TimeSeriesId timeSeriesId, String range);

    Instant getSystemTime();
}
