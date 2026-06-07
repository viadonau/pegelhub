package at.pegelhub.measurement.persistence;


import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.measurement.domain.MeasurementAverage;
import at.pegelhub.timeseries.domain.TimeSeriesId;

import java.time.Instant;
import java.util.List;

/**
 * Repository for all {@code Measurement}s.
 */
public interface MeasurementRepository {

    /**
     * Saves multiple to the repository.
     *
     * @param measurements to save.
     */
    void storeMeasurements(List<Measurement> measurements);

    /**
     * Queries measurements from the repository.
     *
     * @param timeSeriesId of the TimeSeries.
     * @param range in which the returned values reside.
     * @return the measurements for that TimeSeries in that range.
     */
    List<Measurement> getByTimeSeriesIdAndRange(TimeSeriesId timeSeriesId, String range);

    /**
     * Queries the last measurement from the repository.
     *
     * @param timeSeriesId of the TimeSeries.
     * @return the latest measurement for that TimeSeries.
     */
    Measurement getLatestByTimeSeriesId(TimeSeriesId timeSeriesId);

    /**
     * Calculates the average value for a TimeSeries over a given time range.
     *
     * @param timeSeriesId of the TimeSeries.
     * @param range in which to calculate the average.
     * @return the aggregate average for that TimeSeries and range.
     */
    MeasurementAverage getAverageByTimeSeriesIdAndRange(TimeSeriesId timeSeriesId, String range);

    Instant getSystemTime();
}
