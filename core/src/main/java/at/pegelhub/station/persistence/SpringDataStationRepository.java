package at.pegelhub.station.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataStationRepository extends JpaRepository<JpaStation, UUID> {
}
