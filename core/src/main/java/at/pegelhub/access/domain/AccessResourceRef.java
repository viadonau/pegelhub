package at.pegelhub.access.domain;

import at.pegelhub.station.domain.StationId;
import at.pegelhub.timeseries.domain.TimeSeriesId;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record AccessResourceRef(AccessResourceType type, UUID id) {

    public AccessResourceRef {
        requireNonNull(type);
        requireNonNull(id);
    }

    public static AccessResourceRef station(StationId stationId) {
        requireNonNull(stationId);
        return new AccessResourceRef(AccessResourceType.STATION, stationId.value());
    }

    public static AccessResourceRef timeSeries(TimeSeriesId timeSeriesId) {
        requireNonNull(timeSeriesId);
        return new AccessResourceRef(AccessResourceType.TIME_SERIES, timeSeriesId.value());
    }
}
