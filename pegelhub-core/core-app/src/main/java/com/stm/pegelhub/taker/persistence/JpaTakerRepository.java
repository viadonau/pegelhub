package com.stm.pegelhub.taker.persistence;

import com.stm.pegelhub.taker.persistence.JpaTaker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@code Taker}s.
 */

@Repository
public interface JpaTakerRepository extends JpaRepository<JpaTaker, UUID> {

    Optional<JpaTaker> findFirstByStationNumber(String stationNumber);
}
