package com.stm.pegelhub.supplier.persistence;

import com.stm.pegelhub.supplier.persistence.JpaSupplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@code Supplier}s.
 */

@Repository
public interface JpaSupplierRepository extends JpaRepository<JpaSupplier, UUID> {

    Optional<JpaSupplier> findFirstByStationNumber(String stationNumber);

    @Query(value = "SELECT s.id FROM JpaSupplier s, JpaConnector c WHERE s.connector.id = c.id AND c.apiToken = ?1 ")
    UUID getSupplier(UUID authId);

    @Query(value = "SELECT s.connector.id FROM JpaSupplier s where s.id = ?1 ")
    UUID getConnectorID(UUID uuid);
}
