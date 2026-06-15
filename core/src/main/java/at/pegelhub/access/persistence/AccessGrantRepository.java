package at.pegelhub.access.persistence;

import at.pegelhub.access.domain.AccessGrant;
import at.pegelhub.access.domain.AccessGrantId;
import at.pegelhub.connector.domain.ConnectorId;

import java.util.List;
import java.util.Optional;

public interface AccessGrantRepository {

    AccessGrant save(AccessGrant accessGrant);

    Optional<AccessGrant> findById(AccessGrantId id);

    List<AccessGrant> findAll();

    List<AccessGrant> findByConnectorId(ConnectorId connectorId);
}
