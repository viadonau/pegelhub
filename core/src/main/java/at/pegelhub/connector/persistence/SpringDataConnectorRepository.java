package at.pegelhub.connector.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
            select count(connector) > 0
            from ConnectorEntity connector
            where connector.manufacturer.id = :contactId
               or connector.softwareManufacturer.id = :contactId
               or connector.technicallyResponsible.id = :contactId
               or connector.operatingCompany.id = :contactId
            """)
    boolean existsReferencingContact(@Param("contactId") UUID contactId);
}
