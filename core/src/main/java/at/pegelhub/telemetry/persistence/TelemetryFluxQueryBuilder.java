package at.pegelhub.telemetry.persistence;

import at.pegelhub.shared.duration.PegelhubDurationLiteral;
import at.pegelhub.shared.influx.DatabaseProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static at.pegelhub.shared.validation.Validations.requireNotEmpty;
import static java.util.Objects.requireNonNull;

@Component
final class TelemetryFluxQueryBuilder {

    private final DatabaseProperties database;

    TelemetryFluxQueryBuilder(@Qualifier("telemetryConfiguration") DatabaseProperties database) {
        this.database = requireNonNull(database);
    }

    String range(PegelhubDurationLiteral range) {
        requireNonNull(range);
        return from() + " |> range(start: -" + range + ")";
    }

    String latestTelemetry(UUID measurement, PegelhubDurationLiteral latestRange) {
        return range(latestRange) + measurementFilter(measurement) + " |> last()";
    }

    private String from() {
        return "from(bucket: " + stringLiteral(database.bucket()) + ")";
    }

    private String measurementFilter(UUID measurement) {
        requireNonNull(measurement);
        return " |> filter(fn: (r) => r._measurement == " + stringLiteral(measurement.toString()) + ")";
    }

    private String stringLiteral(String value) {
        requireNotEmpty(value);
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
