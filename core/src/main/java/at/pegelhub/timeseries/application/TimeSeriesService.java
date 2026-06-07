package at.pegelhub.timeseries.application;

import at.pegelhub.station.domain.StationId;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;

import java.util.List;

public interface TimeSeriesService {

    TimeSeries create(CreateTimeSeriesCommand command);

    TimeSeries get(TimeSeriesId id);

    List<TimeSeries> list();

    List<TimeSeries> listForStation(StationId stationId);
}
