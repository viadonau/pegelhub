package at.pegelhub.measuringpoint.application;

import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.measuringpoint.persistence.MeasuringPointRepository;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.station.application.StationService;
import at.pegelhub.station.domain.StationId;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.Objects.requireNonNull;

@Service
class MeasuringPointServiceImpl implements MeasuringPointService {

    private final MeasuringPointRepository measuringPoints;
    private final StationService stations;

    MeasuringPointServiceImpl(MeasuringPointRepository measuringPoints, StationService stations) {
        this.measuringPoints = requireNonNull(measuringPoints);
        this.stations = requireNonNull(stations);
    }

    @Override
    public MeasuringPoint create(CreateMeasuringPointCommand command) {
        requireNonNull(command);
        stations.get(command.stationId());
        return measuringPoints.save(MeasuringPoint.create(
                command.stationId(),
                command.name(),
                command.referenceLevel(),
                command.referenceYear(),
                command.riverKilometer(),
                command.bank(),
                command.rnw(),
                command.mw(),
                command.hsw(),
                command.hw100()));
    }

    @Override
    public MeasuringPoint get(MeasuringPointId id) {
        requireNonNull(id);
        return measuringPoints.findById(id)
                .orElseThrow(() -> new NotFoundException("Measuring point not found: " + id.value()));
    }

    @Override
    public List<MeasuringPoint> list() {
        return measuringPoints.findAll();
    }

    @Override
    public List<MeasuringPoint> listForStation(StationId stationId) {
        requireNonNull(stationId);
        stations.get(stationId);
        return measuringPoints.findByStationId(stationId);
    }
}
