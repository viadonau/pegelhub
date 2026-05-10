package at.pegelhub.connector.application;

import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.persistence.ConnectorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Default implementation for {@code ConnectorService}.
 */
@Service
public final class ConnectorServiceImpl implements ConnectorService {

    private final ConnectorRepository connectorRepository;

    public ConnectorServiceImpl(ConnectorRepository connectorRepository) {
        this.connectorRepository = requireNonNull(connectorRepository);
    }

    /**
     * @param connector to save.
     * @return the saved {@link Connector}
     */
    @Override
    public Connector createConnector(Connector connector) {
        return connectorRepository.saveConnector(connector);
    }

    /**
     * @param uuid {@link UUID} of the connector.
     * @return the corresponding {@link Connector} to the specified {@link UUID}
     */
    @Override
    public Connector getConnectorById(UUID uuid) {
        return connectorRepository.getById(uuid);
    }

    /**
      * @return all saved {@link Connector}s
     */
    @Override
    public List<Connector> getAllConnectors() {
        return connectorRepository.getAllConnectors();
    }

    /**
     * @param uuid of the connector to delete.
     */
    @Override
    public void deleteConnector(UUID uuid) {
        connectorRepository.deleteConnector(uuid);

    }
}
