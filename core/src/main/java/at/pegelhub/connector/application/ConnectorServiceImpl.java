package at.pegelhub.connector.application;

import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorStatus;
import at.pegelhub.connector.persistence.ConnectorRepository;
import at.pegelhub.contact.persistence.ContactRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static at.pegelhub.connector.application.ContactUtil.updateConnector;
import static java.util.Objects.requireNonNull;

/**
 * Default implementation for {@code ConnectorService}.
 */
@Service
public final class ConnectorServiceImpl implements ConnectorService {

    private final ConnectorRepository connectorRepository;
    private final ContactRepository contactRepository;

    public ConnectorServiceImpl(ConnectorRepository connectorRepository, ContactRepository contactRepository) {
        this.connectorRepository = requireNonNull(connectorRepository);
        this.contactRepository = requireNonNull(contactRepository);
    }

    /**
     * @param connector to save.
     * @return the saved {@link Connector}
     */
    @Override
    public Connector createConnector(Connector connector) {
        return connectorRepository.saveConnector(updateConnector(contactRepository, null, connector));
    }

    @Override
    public Connector registerConnector(String keycloakClientId, ConnectorStatus status, Connector connector) {
        requireKeycloakClientId(keycloakClientId);
        connectorRepository.findByKeycloakClientId(keycloakClientId).ifPresent(existing -> {
            throw new IllegalArgumentException("Connector already exists for Keycloak client id " + keycloakClientId);
        });
        Connector connectorWithContacts = updateConnector(contactRepository, null, connector);
        return connectorRepository.saveConnector(connectorWithContacts.withExternalAuth(keycloakClientId, status));
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

    private void requireKeycloakClientId(String keycloakClientId) {
        if (keycloakClientId == null || keycloakClientId.isBlank()) {
            throw new IllegalArgumentException("keycloakClientId must not be blank");
        }
    }
}
