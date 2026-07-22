package at.pegelhub.measuringpoint.persistence;

import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.station.domain.StationId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Repository
class MeasuringPointRepositoryAdapter implements MeasuringPointRepository {

    private final SpringDataMeasuringPointRepository measuringPoints;

    MeasuringPointRepositoryAdapter(SpringDataMeasuringPointRepository measuringPoints) {
        this.measuringPoints = requireNonNull(measuringPoints);
    }

    @Override
    public MeasuringPoint save(MeasuringPoint measuringPoint) {
        requireNonNull(measuringPoint);
        return toDomain(measuringPoints.save(toEntity(measuringPoint)));
    }

    @Override
    public Optional<MeasuringPoint> findById(MeasuringPointId id) {
        requireNonNull(id);
        return measuringPoints.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<MeasuringPoint> findAll() {
        return measuringPoints.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<MeasuringPoint> findByStationId(StationId stationId) {
        requireNonNull(stationId);
        return measuringPoints.findByStationId(stationId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    private MeasuringPointEntity toEntity(MeasuringPoint measuringPoint) {
        return new MeasuringPointEntity(
                measuringPoint.id().value(),
                measuringPoint.stationId().value(),
                measuringPoint.name(),
                measuringPoint.referenceLevel(),
                measuringPoint.referenceYear(),
                measuringPoint.riverKilometer(),
                measuringPoint.bank(),
                measuringPoint.rnw(),
                measuringPoint.mw(),
                measuringPoint.hsw(),
                measuringPoint.hw100());
    }

    private MeasuringPoint toDomain(MeasuringPointEntity measuringPoint) {
        return new MeasuringPoint(
                new MeasuringPointId(measuringPoint.id()),
                new StationId(measuringPoint.stationId()),
                measuringPoint.name(),
                measuringPoint.referenceLevel(),
                measuringPoint.referenceYear(),
                measuringPoint.riverKilometer(),
                measuringPoint.bank(),
                measuringPoint.rnw(),
                measuringPoint.mw(),
                measuringPoint.hsw(),
                measuringPoint.hw100());
    }
}
