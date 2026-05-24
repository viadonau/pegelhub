package at.pegelhub.taker.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@code Taker}s.
 */

@Repository
public interface JpaTakerRepository extends JpaRepository<JpaTaker, UUID> {

    Optional<JpaTaker> findFirstByStationNumber(String stationNumber);

    @Query(value = "SELECT t FROM JpaTaker t WHERE t.connector.keycloakClientId = ?1")
    Optional<JpaTaker> findByConnectorKeycloakClientId(String keycloakClientId);
}
