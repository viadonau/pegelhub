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

        // Influx identity is measurement + tags + timestamp. Keep legacy connector tags unchanged
        // and never tag internal points with a run ID, or repeat output would create extra points.
        var point = Point.measurement(measurement.timeSeriesId().value().toString())
                .time(measurement.observedAt(), WritePrecision.MS)
                .addField(InfluxMeasurementSchema.VALUE_FIELD, measurement.value())
                .addField(InfluxMeasurementSchema.RECEIVED_AT_FIELD, measurement.receivedAt().toString());

        if (measurement.submittedByConnectorId() != null) {
            return point.addTag(InfluxMeasurementSchema.SUBMITTED_BY_CONNECTOR_ID_TAG,
                    measurement.submittedByConnectorId().value().toString());
        }

        return point.addTag(InfluxMeasurementSchema.SUBMITTED_BY_INTERNAL_PRODUCER_ID_TAG,
                measurement.submittedByInternalProducerId().value().toString());
    }
}
