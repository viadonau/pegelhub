package at.pegelhub.measuringpoint.persistence;

import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.station.domain.StationId;

import java.util.List;
import java.util.Optional;

public interface MeasuringPointRepository {

    MeasuringPoint save(MeasuringPoint measuringPoint);

    Optional<MeasuringPoint> findById(MeasuringPointId id);

    List<MeasuringPoint> findAll();

    List<MeasuringPoint> findByStationId(StationId stationId);
}
