package at.pegelhub.measuringpoint.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataMeasuringPointRepository extends JpaRepository<MeasuringPointEntity, UUID> {

    List<MeasuringPointEntity> findByStationId(UUID stationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select measuringPoint from MeasuringPointEntity measuringPoint where measuringPoint.id = :id")
    Optional<MeasuringPointEntity> findByIdForUpdate(@Param("id") UUID id);
}
