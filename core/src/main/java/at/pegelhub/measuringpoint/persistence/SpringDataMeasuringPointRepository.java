package at.pegelhub.measuringpoint.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataMeasuringPointRepository extends JpaRepository<MeasuringPointEntity, UUID> {

    List<MeasuringPointEntity> findByStationId(UUID stationId);
}
