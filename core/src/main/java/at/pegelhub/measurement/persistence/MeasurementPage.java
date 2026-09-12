package at.pegelhub.measurement.persistence;

import at.pegelhub.measurement.application.MeasurementReadRow;

import java.util.List;

/**
 * Values returned by the repository, still in storage units. The application service converts them
 * and adds the representation and unit to the response.
 *
 * @param truncated true if matching measurements were left out because of the limit;
 *                  a full page alone does not mean it was truncated
 */
public record MeasurementPage(boolean truncated, List<MeasurementReadRow> measurements) {

    public MeasurementPage {
        measurements = List.copyOf(measurements);
    }
}
