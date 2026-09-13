package at.pegelhub.timeseries.application;

import at.pegelhub.measurement.domain.InternalProducerId;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;

import java.util.List;

public interface TimeSeriesService {

    TimeSeries create(CreateTimeSeriesCommand command);

    TimeSeries update(TimeSeriesId id, UpdateTimeSeriesCommand command);

    /**
     * Claims or releases exclusive canonical output ownership, never displacing a different source.
     * Internal callers must also maintain producer bindings; this is not exposed by the metadata HTTP controller.
     */
    void assignInternalProducer(TimeSeriesId id, InternalProducerId producerId, boolean assign);

    TimeSeries get(TimeSeriesId id);

    List<TimeSeries> list();

    List<TimeSeries> listForMeasuringPoint(MeasuringPointId measuringPointId);

    List<TimeSeries> listForStation(StationId stationId);
}
