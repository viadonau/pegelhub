package at.pegelhub.access.application;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.timeseries.domain.TimeSeriesId;

public interface ConnectorReadAccessService {
    void grantStation(ConnectorId connectorId, StationId stationId);
    void revokeStation(ConnectorId connectorId, StationId stationId);
    void grantTimeSeries(ConnectorId connectorId, TimeSeriesId timeSeriesId);
    void revokeTimeSeries(ConnectorId connectorId, TimeSeriesId timeSeriesId);
    boolean allows(ConnectorId connectorId, TimeSeriesId timeSeriesId);
}
