package at.pegelhub.access.application;

import at.pegelhub.access.domain.AccessGrant;
import at.pegelhub.access.domain.AccessGrantId;
import at.pegelhub.connector.domain.ConnectorId;

import java.util.List;

public interface AccessGrantService {

    AccessGrant create(CreateAccessGrantCommand command);

    AccessGrant get(AccessGrantId id);

    List<AccessGrant> list();

    List<AccessGrant> listForConnector(ConnectorId connectorId);
}
