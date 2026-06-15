package at.pegelhub.measurement.api;

import at.pegelhub.measurement.domain.WriteMeasurement;
import at.pegelhub.measurement.domain.WriteMeasurements;
import at.pegelhub.timeseries.domain.TimeSeriesId;

import java.util.ArrayList;
import java.util.List;

/**
 * Provider, which has methods to convert dtos to domain objects.
 */
final class DtoToDomainConverter {

    static WriteMeasurements convert(WriteMeasurementsDto measurementsDto) {
        return new WriteMeasurements(convert(measurementsDto.measurements()));
    }

    private static List<WriteMeasurement> convert(List<WriteMeasurementDto> measurementsDto) {
        List<WriteMeasurement> measurements = new ArrayList<>(measurementsDto.size());
        for (WriteMeasurementDto measurementDto : measurementsDto) {
            measurements.add(convert(measurementDto));
        }
        return measurements;
    }

    private static WriteMeasurement convert(WriteMeasurementDto measurementDto) {
        return new WriteMeasurement(
                new TimeSeriesId(measurementDto.timeSeriesId()),
                measurementDto.observedAt(),
                measurementDto.value());
    }
}
