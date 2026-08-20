package at.pegelhub.measuringpoint.application;

import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.measuringpoint.persistence.MeasuringPointRepository;
import at.pegelhub.timeseries.persistence.TimeSeriesRepository;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.shared.error.MetadataConflictException;
import at.pegelhub.station.application.StationService;
import at.pegelhub.station.domain.StationId;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.Objects.requireNonNull;

@Service
class MeasuringPointServiceImpl implements MeasuringPointService {

    private final MeasuringPointRepository measuringPoints;
    private final StationService stations;
    private final TimeSeriesRepository timeSeries;

    MeasuringPointServiceImpl(MeasuringPointRepository measuringPoints, StationService stations, TimeSeriesRepository timeSeries) {
        this.measuringPoints = requireNonNull(measuringPoints);
        this.stations = requireNonNull(stations);
        this.timeSeries = requireNonNull(timeSeries);
    }

    @Override
    public MeasuringPoint create(CreateMeasuringPointCommand command) {
        requireNonNull(command);
        stations.get(command.stationId());
        return measuringPoints.save(MeasuringPoint.create(
                command.stationId(),
                command.name(),
                command.status(), command.position(), command.gaugeZeroElevationMAboveAdria(), command.waterLevelReferences()));
    }

    @Override
    @Transactional
    public MeasuringPoint update(MeasuringPointId id, UpdateMeasuringPointCommand command) {
        requireNonNull(command);
        var existing = getForUpdate(id);
        if (command.gaugeZeroElevationMAboveAdria() == null
                && existing.gaugeZeroElevationMAboveAdria() != null
                && timeSeries.hasAbsoluteSourceFor(id)) {
            throw new MetadataConflictException("Cannot remove gauge zero elevation while an absolute source depends on it");
        }
        return measuringPoints.save(existing.update(
                command.name(),
                command.status(), command.position(), command.gaugeZeroElevationMAboveAdria(), command.waterLevelReferences()));
    }

    @Override
    public MeasuringPoint get(MeasuringPointId id) {
        requireNonNull(id);
        return measuringPoints.findById(id)
                .orElseThrow(() -> new NotFoundException("Measuring point not found: " + id.value()));
    }

    @Override
    public MeasuringPoint getForUpdate(MeasuringPointId id) {
        requireNonNull(id);
        return measuringPoints.findByIdForUpdate(id)
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
