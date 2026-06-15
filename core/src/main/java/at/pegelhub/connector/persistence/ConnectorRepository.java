package at.pegelhub.connector.persistence;

import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorId;

import java.util.List;
import java.util.Optional;

public interface ConnectorRepository {

    Connector save(Connector connector);

    Optional<Connector> findById(ConnectorId id);

    List<Connector> findAll();

    void delete(ConnectorId id);

    Optional<Connector> findByConnectorNumber(String connectorNumber);

    Optional<Connector> findByKeycloakClientId(String keycloakClientId);
}
