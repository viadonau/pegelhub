package at.pegelhub.supplier.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * JPA repository for {@code StationManufacturer}s.
 */

@Repository
public interface JpaStationManufacturerRepository extends JpaRepository<JpaStationManufacturer, UUID> {
}
