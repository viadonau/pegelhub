package at.pegelhub.stationowner.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataStationOwnerRepository extends JpaRepository<StationOwnerEntity, UUID> {
}
