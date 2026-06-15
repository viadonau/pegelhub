package at.pegelhub.timeseries.persistence;

import at.pegelhub.station.domain.StationId;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;

import java.util.List;
import java.util.Optional;

public interface TimeSeriesRepository {

    TimeSeries save(TimeSeries timeSeries);

    Optional<TimeSeries> findById(TimeSeriesId id);

    List<TimeSeries> findAll();

    List<TimeSeries> findByStationId(StationId stationId);
}
