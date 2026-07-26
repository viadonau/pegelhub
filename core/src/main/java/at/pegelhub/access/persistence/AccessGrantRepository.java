package at.pegelhub.access.persistence;

import at.pegelhub.access.domain.AccessGrant;
import at.pegelhub.access.domain.AccessGrantId;
import at.pegelhub.access.domain.AccessPermission;
import at.pegelhub.access.domain.AccessResourceRef;
import at.pegelhub.connector.domain.ConnectorId;

import java.util.List;
import java.util.Optional;

public interface AccessGrantRepository {

    AccessGrant save(AccessGrant accessGrant);

    AccessGrant saveOrFindByAssignment(AccessGrant accessGrant);

    Optional<AccessGrant> findById(AccessGrantId id);

    Optional<AccessGrant> findByAssignment(
            ConnectorId connectorId,
            AccessResourceRef resource,
            AccessPermission permission);

    List<AccessGrant> findAll();

    List<AccessGrant> findByConnectorId(ConnectorId connectorId);
}
