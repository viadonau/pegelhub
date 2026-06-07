package at.pegelhub.access.application;

import at.pegelhub.access.domain.AccessGrant;
import at.pegelhub.access.domain.AccessGrantId;
import at.pegelhub.access.domain.AccessResourceType;
import at.pegelhub.access.persistence.AccessGrantRepository;
import at.pegelhub.connector.application.ConnectorService;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.station.application.StationService;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import at.pegelhub.timeseries.application.TimeSeriesService;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.Objects.requireNonNull;

@Service
final class AccessGrantServiceImpl implements AccessGrantService {

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
        connectors.getConnectorById(command.connectorId().value());
        ensureResourceExists(command);
        return accessGrants.save(AccessGrant.create(
                command.connectorId(),
                command.resource(),
                command.permission(),
                command.validFrom(),
                command.validUntil(),
                command.includeFutureTimeSeries()));
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
        connectors.getConnectorById(connectorId.value());
        return accessGrants.findByConnectorId(connectorId);
    }

    private void ensureResourceExists(CreateAccessGrantCommand command) {
        if (command.resource().type() == AccessResourceType.STATION) {
            stations.get(new StationId(command.resource().id()));
            return;
        }
        timeSeries.get(new TimeSeriesId(command.resource().id()));
    }
}
