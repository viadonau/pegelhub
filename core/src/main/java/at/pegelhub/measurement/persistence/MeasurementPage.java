package at.pegelhub.measurement.persistence;

import at.pegelhub.measurement.application.MeasurementReadRow;

import java.util.List;

/**
 * Repository values are always canonical; application reads attach representation metadata.
 */
public record MeasurementPage(boolean truncated, List<MeasurementReadRow> measurements) {

    public MeasurementPage {
        measurements = List.copyOf(measurements);
    }
}
