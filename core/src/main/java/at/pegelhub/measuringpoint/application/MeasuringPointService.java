package at.pegelhub.measuringpoint.application;

import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.station.domain.StationId;

import java.util.List;

public interface MeasuringPointService {

    MeasuringPoint create(CreateMeasuringPointCommand command);

    MeasuringPoint get(MeasuringPointId id);

    List<MeasuringPoint> list();

    List<MeasuringPoint> listForStation(StationId stationId);
}
