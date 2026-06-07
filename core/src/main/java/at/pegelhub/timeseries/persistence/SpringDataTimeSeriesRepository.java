package at.pegelhub.timeseries.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataTimeSeriesRepository extends JpaRepository<JpaTimeSeries, UUID> {

    List<JpaTimeSeries> findByStationId(UUID stationId);
}
