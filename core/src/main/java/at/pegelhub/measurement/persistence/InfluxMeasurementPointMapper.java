package at.pegelhub.measurement.persistence;

import at.pegelhub.measurement.domain.Measurement;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Component
final class InfluxMeasurementPointMapper {

    List<Point> toPoints(List<Measurement> measurements) {
        requireNonNull(measurements);
        List<Point> points = new ArrayList<>(measurements.size());
        for (Measurement measurement : measurements) {
            points.add(toPoint(measurement));
        }
        return points;
    }

    Point toPoint(Measurement measurement) {
        requireNonNull(measurement);
        return Point.measurement(measurement.timeSeriesId().value().toString())
                .time(measurement.observedAt(), WritePrecision.MS)
                .addTag(InfluxMeasurementSchema.SUBMITTED_BY_CONNECTOR_ID_TAG,
                        measurement.submittedByConnectorId().value().toString())
                .addField(InfluxMeasurementSchema.VALUE_FIELD, measurement.value())
                .addField(InfluxMeasurementSchema.RECEIVED_AT_FIELD, measurement.receivedAt().toString());
    }
}
