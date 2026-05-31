package at.pegelhub.shared.influx;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Map;

import static at.pegelhub.shared.validation.Validations.requireNotEmpty;
import static java.util.Objects.requireNonNull;

/**
 * A single logical Influx point after Flux rows for different fields have been grouped.
 */
public record InfluxPoint(
        String measurement,
        @Nullable Instant timestamp,
        Map<String, Object> fields,
        Map<String, String> tags) {

    public InfluxPoint {
        requireNotEmpty(measurement);
        fields = Map.copyOf(requireNotEmpty(fields));
        tags = Map.copyOf(requireNonNull(tags));
    }

    public boolean isAggregate() {
        return timestamp == null;
    }
}
