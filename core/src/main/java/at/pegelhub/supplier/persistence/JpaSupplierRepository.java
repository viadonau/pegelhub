package at.pegelhub.supplier.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@code Supplier}s.
 */

@Repository
public interface JpaSupplierRepository extends JpaRepository<JpaSupplier, UUID> {

    Optional<JpaSupplier> findFirstByStationNumber(String stationNumber);

    @Query(value = "SELECT s FROM JpaSupplier s WHERE s.connector.keycloakClientId = ?1")
    Optional<JpaSupplier> findByConnectorKeycloakClientId(String keycloakClientId);

    @Query(value = "SELECT s.connector.id FROM JpaSupplier s where s.id = ?1 ")
    UUID getConnectorID(UUID uuid);
}
