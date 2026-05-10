package com.stm.pegelhub.taker.persistence;

import com.stm.pegelhub.taker.persistence.JpaTakerServiceManufacturer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * JPA repository for {@code ServiceManufacturer}s.
 */

@Repository
public interface JpaTakerServiceManufacturerRepository extends JpaRepository<JpaTakerServiceManufacturer, UUID> {
}
