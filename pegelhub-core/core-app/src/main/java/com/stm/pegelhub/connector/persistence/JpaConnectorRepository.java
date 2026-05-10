package com.stm.pegelhub.connector.persistence;

import com.stm.pegelhub.connector.persistence.JpaConnector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@code Connector}s.
 */

@Repository
public interface JpaConnectorRepository extends JpaRepository<JpaConnector, UUID> {
    Optional<JpaConnector> findFirstByConnectorNumber(String connectorNumber);

    @Query(value = "SELECT c.apiToken from JpaConnector as c where c.id = ?1 ")
    Optional<JpaConnector> findByUUID(UUID uuid);
}
