package at.pegelhub.access.application;

import at.pegelhub.access.persistence.ConnectorStationReadAccessRepository;
import at.pegelhub.access.persistence.ConnectorTimeSeriesReadAccessRepository;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.connector.persistence.ConnectorRepository;
import at.pegelhub.measuringpoint.application.MeasuringPointService;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.station.application.StationService;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.timeseries.application.TimeSeriesService;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Objects.requireNonNull;

@Service
class ConnectorReadAccessServiceImpl implements ConnectorReadAccessService {
    private final ConnectorRepository connectors;
    private final StationService stations;
    private final TimeSeriesService timeSeries;
    private final MeasuringPointService measuringPoints;
    private final ConnectorStationReadAccessRepository stationAccess;
    private final ConnectorTimeSeriesReadAccessRepository timeSeriesAccess;

    ConnectorReadAccessServiceImpl(ConnectorRepository connectors, StationService stations, TimeSeriesService timeSeries,
                                   MeasuringPointService measuringPoints, ConnectorStationReadAccessRepository stationAccess,
                                   ConnectorTimeSeriesReadAccessRepository timeSeriesAccess) {
        this.connectors = requireNonNull(connectors);
        this.stations = requireNonNull(stations);
        this.timeSeries = requireNonNull(timeSeries);
        this.measuringPoints = requireNonNull(measuringPoints);
        this.stationAccess = requireNonNull(stationAccess);
        this.timeSeriesAccess = requireNonNull(timeSeriesAccess);
    }

    @Override @Transactional
    public void grantStation(ConnectorId connectorId, StationId stationId) {
        requireResources(connectorId, stationId);
        stationAccess.insertIfAbsent(connectorId.value(), stationId.value());
    }

    @Override @Transactional
    public void revokeStation(ConnectorId connectorId, StationId stationId) {
        requireResources(connectorId, stationId);
        stationAccess.deleteByConnectorIdAndStationId(connectorId.value(), stationId.value());
    }

    @Override @Transactional
    public void grantTimeSeries(ConnectorId connectorId, TimeSeriesId timeSeriesId) {
        requireConnector(connectorId);
        timeSeries.get(timeSeriesId);
        timeSeriesAccess.insertIfAbsent(connectorId.value(), timeSeriesId.value());
    }

    @Override @Transactional
    public void revokeTimeSeries(ConnectorId connectorId, TimeSeriesId timeSeriesId) {
        requireConnector(connectorId);
        timeSeries.get(timeSeriesId);
        timeSeriesAccess.deleteByConnectorIdAndTimeSeriesId(connectorId.value(), timeSeriesId.value());
    }

    @Override
    public boolean allows(ConnectorId connectorId, TimeSeriesId timeSeriesId) {
        var series = timeSeries.get(timeSeriesId);
        var stationId = measuringPoints.get(series.measuringPointId()).stationId().value();
        return timeSeriesAccess.existsByConnectorIdAndTimeSeriesId(connectorId.value(), timeSeriesId.value())
                || stationAccess.existsByConnectorIdAndStationId(connectorId.value(), stationId);
    }

    private void requireResources(ConnectorId connectorId, StationId stationId) {
        requireConnector(connectorId);
        stations.get(stationId);
    }

    private void requireConnector(ConnectorId connectorId) {
        connectors.findById(connectorId)
                .orElseThrow(() -> new NotFoundException("Connector not found: " + connectorId.value()));
    }
}
