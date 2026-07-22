package at.pegelhub.timeseries.application;

import at.pegelhub.connector.application.ConnectorService;
import at.pegelhub.measuringpoint.application.MeasuringPointService;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.station.application.StationService;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import at.pegelhub.timeseries.persistence.TimeSeriesRepository;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.Objects.requireNonNull;

@Service
class TimeSeriesServiceImpl implements TimeSeriesService {

    private final TimeSeriesRepository timeSeries;
    private final MeasuringPointService measuringPoints;
    private final StationService stations;
    private final ConnectorService connectors;

    TimeSeriesServiceImpl(
            TimeSeriesRepository timeSeries,
            MeasuringPointService measuringPoints,
            StationService stations,
            ConnectorService connectors) {
        this.timeSeries = requireNonNull(timeSeries);
        this.measuringPoints = requireNonNull(measuringPoints);
        this.stations = requireNonNull(stations);
        this.connectors = requireNonNull(connectors);
    }

    @Override
    public TimeSeries create(CreateTimeSeriesCommand command) {
        requireNonNull(command);
        measuringPoints.get(command.measuringPointId());
        if (command.sourceConnectorId() != null) {
            connectors.get(command.sourceConnectorId());
        }
        return timeSeries.save(TimeSeries.create(
                command.measuringPointId(),
                command.observedProperty(),
                command.unit(),
                command.externalCode(),
                command.sourceConnectorId()));
    }

    @Override
    public TimeSeries get(TimeSeriesId id) {
        requireNonNull(id);
        return timeSeries.findById(id)
                .orElseThrow(() -> new NotFoundException("Time series not found: " + id.value()));
    }

    @Override
    public List<TimeSeries> list() {
        return timeSeries.findAll();
    }

    @Override
    public List<TimeSeries> listForMeasuringPoint(MeasuringPointId measuringPointId) {
        requireNonNull(measuringPointId);
        measuringPoints.get(measuringPointId);
        return timeSeries.findByMeasuringPointId(measuringPointId);
    }

    @Override
    public List<TimeSeries> listForStation(StationId stationId) {
        requireNonNull(stationId);
        stations.get(stationId);
        return timeSeries.findByStationId(stationId);
    }
}
