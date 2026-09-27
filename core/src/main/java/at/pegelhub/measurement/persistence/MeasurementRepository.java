package at.pegelhub.measurement.persistence;

import at.pegelhub.measurement.application.MeasurementListQuery;
import at.pegelhub.measurement.application.MeasurementReadRow;
import at.pegelhub.measurement.application.MeasurementWindow;
import at.pegelhub.measurement.application.LatestMeasurement;
import at.pegelhub.measurement.application.MeasurementLatestQuery;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.timeseries.domain.TimeSeriesId;

import java.time.Instant;
import java.util.List;

/**
 * Reads and writes values in canonical storage units. The application service handles conversion.
 * A representation in a read query does not change the units returned here.
 */
public interface MeasurementRepository {

    /**
     * The caller must check permissions and convert values to storage units before calling this method.
     */
    void storeMeasurements(List<Measurement> measurements);

    MeasurementPage listMeasurements(MeasurementListQuery query);

    /**
     * Returns the latest retained observation from each writer before the window plus all rows in it.
     * The row budget applies to the combined evidence; fail rather than return partial input.
     */
    List<MeasurementReadRow> listIntervalEvidence(
            TimeSeriesId timeSeriesId, MeasurementWindow window, int maxRows);

    List<LatestMeasurement> listLatestMeasurements(MeasurementLatestQuery query);

    Instant getSystemTime();
}
