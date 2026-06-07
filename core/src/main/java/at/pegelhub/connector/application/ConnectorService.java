package at.pegelhub.connector.application;

import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.connector.domain.ConnectorStatus;

import java.util.List;

public interface ConnectorService {

    Connector create(CreateConnectorCommand command);

    Connector register(String keycloakClientId, ConnectorStatus status, CreateConnectorCommand command);

    Connector get(ConnectorId id);

    List<Connector> list();

    void delete(ConnectorId id);
}
