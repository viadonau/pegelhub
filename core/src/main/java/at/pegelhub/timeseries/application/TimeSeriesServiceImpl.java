package at.pegelhub.timeseries.application;

import at.pegelhub.connector.application.ConnectorService;
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
    private final StationService stations;
    private final ConnectorService connectors;

    TimeSeriesServiceImpl(TimeSeriesRepository timeSeries, StationService stations, ConnectorService connectors) {
        this.timeSeries = requireNonNull(timeSeries);
        this.stations = requireNonNull(stations);
        this.connectors = requireNonNull(connectors);
    }

    @Override
    public TimeSeries create(CreateTimeSeriesCommand command) {
        requireNonNull(command);
        stations.get(command.stationId());
        if (command.sourceConnectorId() != null) {
            connectors.get(command.sourceConnectorId());
        }
        return timeSeries.save(TimeSeries.create(
                command.stationId(),
                command.observedProperty(),
                command.unit(),
                command.referenceLevel(),
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
    public List<TimeSeries> listForStation(StationId stationId) {
        requireNonNull(stationId);
        stations.get(stationId);
        return timeSeries.findByStationId(stationId);
    }
}
