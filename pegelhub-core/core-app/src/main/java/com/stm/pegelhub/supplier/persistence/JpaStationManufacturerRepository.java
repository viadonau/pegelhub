package com.stm.pegelhub.supplier.persistence;

import com.stm.pegelhub.supplier.persistence.JpaStationManufacturer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * JPA repository for {@code StationManufacturer}s.
 */

@Repository
public interface JpaStationManufacturerRepository extends JpaRepository<JpaStationManufacturer, UUID> {
}
