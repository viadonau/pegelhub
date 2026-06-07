package at.pegelhub.connector.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@code Connector}s.
 */

@Repository
public interface SpringDataConnectorRepository extends JpaRepository<ConnectorEntity, UUID> {
    Optional<ConnectorEntity> findFirstByConnectorNumber(String connectorNumber);

    Optional<ConnectorEntity> findFirstByKeycloakClientId(String keycloakClientId);
}
