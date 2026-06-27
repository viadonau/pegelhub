package at.pegelhub.measurement.api.write;

import at.pegelhub.measurement.domain.WriteMeasurement;
import at.pegelhub.measurement.domain.WriteMeasurements;
import at.pegelhub.timeseries.domain.TimeSeriesId;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps Measurement write HTTP requests to application input.
 */
public final class MeasurementWriteRequestMapper {

    public static WriteMeasurements convert(WriteMeasurementsRequest request) {
        return new WriteMeasurements(convert(request.measurements()));
    }

    private static List<WriteMeasurement> convert(List<WriteMeasurementRequest> requests) {
        List<WriteMeasurement> measurements = new ArrayList<>(requests.size());
        for (WriteMeasurementRequest request : requests) {
            measurements.add(convert(request));
        }
        return measurements;
    }

    private static WriteMeasurement convert(WriteMeasurementRequest request) {
        return new WriteMeasurement(
                new TimeSeriesId(request.timeSeriesId()),
                request.observedAt(),
                request.value());
    }
}
