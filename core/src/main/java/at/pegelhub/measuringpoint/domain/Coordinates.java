package at.pegelhub.measuringpoint.domain;

import java.math.BigDecimal;

import static java.util.Objects.requireNonNull;

public record Coordinates(BigDecimal latitude, BigDecimal longitude) {

    public Coordinates {
        requireNonNull(latitude, "latitude must not be null");
        requireNonNull(longitude, "longitude must not be null");
        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new IllegalArgumentException("latitude must be between -90 and 90");
        }
        if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new IllegalArgumentException("longitude must be between -180 and 180");
        }
    }
}
