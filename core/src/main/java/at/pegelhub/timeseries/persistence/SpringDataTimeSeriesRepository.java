package at.pegelhub.timeseries.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

interface SpringDataTimeSeriesRepository extends JpaRepository<TimeSeriesEntity, UUID> {

    List<TimeSeriesEntity> findByMeasuringPointId(UUID measuringPointId);

    @Query("""
            select timeSeries
              from TimeSeriesEntity timeSeries, MeasuringPointEntity measuringPoint
             where measuringPoint.id = timeSeries.measuringPointId
               and measuringPoint.stationId = :stationId
            """)
    List<TimeSeriesEntity> findByStationId(UUID stationId);
}
