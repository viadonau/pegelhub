package at.pegelhub.timeseries.domain;

import static java.util.Objects.requireNonNull;

public record ExternalTimeSeriesCode(String value) {

    public ExternalTimeSeriesCode {
        requireNonNull(value);
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("External time series code must not be blank");
        }
    }
}
