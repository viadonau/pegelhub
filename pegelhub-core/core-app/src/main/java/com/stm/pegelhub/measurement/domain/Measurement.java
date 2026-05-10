package com.stm.pegelhub.measurement.domain;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static com.stm.pegelhub.shared.validation.Validations.requireNotEmpty;
import static java.util.Objects.requireNonNull;


/**
 * Data class for measurements which represents an entry in the time series database (InfluxDB) in the "data" (measurement) bucket.
 */
public record Measurement(UUID measurement, LocalDateTime timestamp, Map<String, Double> fields,
                          Map<String, String> infos) {

    public Measurement {
        requireNonNull(measurement);
        requireNonNull(timestamp);
        requireNotEmpty(fields);
        requireNonNull(infos);
    }
}
