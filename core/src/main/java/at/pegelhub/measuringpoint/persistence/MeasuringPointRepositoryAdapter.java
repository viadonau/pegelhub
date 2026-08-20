package at.pegelhub.measuringpoint.persistence;

import at.pegelhub.measuringpoint.domain.BankSide;
import at.pegelhub.measuringpoint.domain.Coordinates;
import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.measuringpoint.domain.MeasuringPointPosition;
import at.pegelhub.measuringpoint.domain.WaterLevelReferences;
import at.pegelhub.shared.metadata.MetadataStatus;
import at.pegelhub.station.domain.StationId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Repository
class MeasuringPointRepositoryAdapter implements MeasuringPointRepository {
    private final SpringDataMeasuringPointRepository measuringPoints;

    MeasuringPointRepositoryAdapter(SpringDataMeasuringPointRepository measuringPoints) { this.measuringPoints = requireNonNull(measuringPoints); }

    @Override public MeasuringPoint save(MeasuringPoint point) { return toDomain(measuringPoints.save(toEntity(point))); }
    @Override public Optional<MeasuringPoint> findById(MeasuringPointId id) { return measuringPoints.findById(id.value()).map(this::toDomain); }
    @Override public Optional<MeasuringPoint> findByIdForUpdate(MeasuringPointId id) { return measuringPoints.findByIdForUpdate(id.value()).map(this::toDomain); }
    @Override public List<MeasuringPoint> findAll() { return measuringPoints.findAll().stream().map(this::toDomain).toList(); }
    @Override public List<MeasuringPoint> findByStationId(StationId stationId) { return measuringPoints.findByStationId(stationId.value()).stream().map(this::toDomain).toList(); }

    private MeasuringPointEntity toEntity(MeasuringPoint point) {
        var position = point.position();
        var coordinates = position == null ? null : position.coordinates();
        var references = point.waterLevelReferences();
        return new MeasuringPointEntity(
                point.id().value(), point.stationId().value(), point.name(), point.status().value(),
                position == null ? null : position.riverKilometer(),
                position == null || position.bank() == null ? null : position.bank().value(),
                coordinates == null ? null : coordinates.latitude(),
                coordinates == null ? null : coordinates.longitude(),
                point.gaugeZeroElevationMAboveAdria(),
                references == null ? null : references.referenceSetYear(),
                references == null ? null : references.rnwCm(),
                references == null ? null : references.mwCm(),
                references == null ? null : references.hswCm(),
                references == null ? null : references.hw100Cm());
    }

    private MeasuringPoint toDomain(MeasuringPointEntity point) {
        Coordinates coordinates = point.latitude() == null ? null : new Coordinates(point.latitude(), point.longitude());
        MeasuringPointPosition position = new MeasuringPointPosition(
                point.riverKilometer(), BankSide.fromNullable(point.bank()), coordinates);
        WaterLevelReferences references = point.referenceSetYear() == null ? null : new WaterLevelReferences(
                point.referenceSetYear(), point.rnwCm(), point.mwCm(), point.hswCm(), point.hw100Cm());
        return new MeasuringPoint(
                new MeasuringPointId(point.id()), new StationId(point.stationId()), point.name(),
                MetadataStatus.from(point.status()), position.isEmpty() ? null : position,
                point.gaugeZeroElevationMAboveAdria(), references);
    }
}
