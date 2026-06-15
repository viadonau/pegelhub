package at.pegelhub.timeseries.domain;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record TimeSeriesId(UUID value) {

    public TimeSeriesId {
        requireNonNull(value);
    }
}
