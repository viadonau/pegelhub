package at.pegelhub.connector.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@code Connector}s.
 */

@Repository
public interface JpaConnectorRepository extends JpaRepository<JpaConnector, UUID> {
    Optional<JpaConnector> findFirstByConnectorNumber(String connectorNumber);
}
