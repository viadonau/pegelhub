package at.pegelhub.connector.application;

import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.shared.metadata.MetadataStatus;

import java.util.List;

public interface ConnectorService {
    Connector create(CreateConnectorCommand command);
    Connector register(String keycloakClientId, MetadataStatus status, CreateConnectorCommand command);
    Connector update(ConnectorId id, UpdateConnectorCommand command);
    Connector get(ConnectorId id);
    List<Connector> list();
}
