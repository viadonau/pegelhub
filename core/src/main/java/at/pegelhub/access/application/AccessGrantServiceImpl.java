package at.pegelhub.access.application;

import at.pegelhub.access.domain.AccessGrant;
import at.pegelhub.access.domain.AccessGrantId;
import at.pegelhub.access.domain.AccessPermission;
import at.pegelhub.access.domain.AccessResourceType;
import at.pegelhub.access.persistence.AccessGrantRepository;
import at.pegelhub.connector.application.ConnectorService;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.station.application.StationService;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import at.pegelhub.timeseries.application.TimeSeriesService;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.Objects.requireNonNull;

@Service
class AccessGrantServiceImpl implements AccessGrantService {

    private final AccessGrantRepository accessGrants;
    private final ConnectorService connectors;
    private final StationService stations;
    private final TimeSeriesService timeSeries;

    AccessGrantServiceImpl(
            AccessGrantRepository accessGrants,
            ConnectorService connectors,
            StationService stations,
            TimeSeriesService timeSeries) {
        this.accessGrants = requireNonNull(accessGrants);
        this.connectors = requireNonNull(connectors);
        this.stations = requireNonNull(stations);
        this.timeSeries = requireNonNull(timeSeries);
    }

    @Override
    public AccessGrant create(CreateAccessGrantCommand command) {
        requireNonNull(command);
        connectors.get(command.connectorId());
        ensureResourceAllowsGrant(command);
        return accessGrants.save(AccessGrant.create(
                command.connectorId(),
                command.resource(),
                command.permission()));
    }

    @Override
    public AccessGrant get(AccessGrantId id) {
        requireNonNull(id);
        return accessGrants.findById(id)
                .orElseThrow(() -> new NotFoundException("Access grant not found: " + id.value()));
    }

    @Override
    public List<AccessGrant> list() {
        return accessGrants.findAll();
    }

    @Override
    public List<AccessGrant> listForConnector(ConnectorId connectorId) {
        requireNonNull(connectorId);
        connectors.get(connectorId);
        return accessGrants.findByConnectorId(connectorId);
    }

    private void ensureResourceAllowsGrant(CreateAccessGrantCommand command) {
        if (command.resource().type() == AccessResourceType.STATION) {
            stations.get(new StationId(command.resource().id()));
            if (command.permission() != AccessPermission.READ) {
                throw new IllegalArgumentException("Station grants only support READ permission");
            }
            return;
        }
        TimeSeries series = timeSeries.get(new TimeSeriesId(command.resource().id()));
        if (command.permission() == AccessPermission.WRITE
                && series.sourceConnectorId() != null
                && !series.sourceConnectorId().equals(command.connectorId())) {
            throw new IllegalArgumentException(
                    "Connector is not the source connector for this TimeSeries");
        }
    }
}
