package at.pegelhub.timeseries.persistence;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measurement.domain.InternalProducerId;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.shared.metadata.MetadataStatus;
import at.pegelhub.timeseries.domain.MeasurementRepresentation;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;
import at.pegelhub.timeseries.domain.SourceAssignment;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Repository
class TimeSeriesRepositoryAdapter implements TimeSeriesRepository {

    private final SpringDataTimeSeriesRepository timeSeries;
    private final EntityManager entityManager;

    TimeSeriesRepositoryAdapter(SpringDataTimeSeriesRepository timeSeries, EntityManager entityManager) {
        this.timeSeries = requireNonNull(timeSeries);
        this.entityManager = requireNonNull(entityManager);
    }

    @Override
    public TimeSeries save(TimeSeries series) {
        // Release the previous assignment before another series claims the same producer's unique ownership.
        return toDomain(timeSeries.saveAndFlush(toEntity(series)));
    }

    @Override
    public Optional<TimeSeries> findById(TimeSeriesId id) {
        return timeSeries.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<TimeSeries> refreshForUpdate(TimeSeriesId id) {
        return timeSeries.findById(id.value()).map(entity -> {
            // Re-read after locking: the pre-lock lookup may have cached ownership that another transaction changed.
            entityManager.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
            return toDomain(entity);
        });
    }

    @Override
    public List<TimeSeries> findAll() {
        return timeSeries.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<TimeSeries> findByMeasuringPointId(MeasuringPointId id) {
        return timeSeries.findByMeasuringPointId(id.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<TimeSeries> findByStationId(StationId id) {
        return timeSeries.findByStationId(id.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean hasAbsoluteSourceFor(MeasuringPointId id) {
        return timeSeries.existsByMeasuringPointIdAndSourceRepresentation(
                id.value(), MeasurementRepresentation.METRES_ABOVE_ADRIA.value());
    }

    private TimeSeriesEntity toEntity(TimeSeries series) {
        SourceAssignment assignment = series.sourceAssignment();

        return new TimeSeriesEntity(
                series.id().value(),
                series.measuringPointId().value(),
                series.observedProperty().value(),
                series.status().value(),
                series.sourceConnectorId() == null ? null : series.sourceConnectorId().value(),
                assignment == null ? null : assignment.representation().value(),
                assignment == null || assignment.internalProducerId() == null ? null
                        : assignment.internalProducerId().value());
    }

    private TimeSeries toDomain(TimeSeriesEntity series) {
        SourceAssignment assignment = series.sourceConnectorId() == null
                ? null
                : new SourceAssignment(
                        new ConnectorId(series.sourceConnectorId()),
                        MeasurementRepresentation.from(series.sourceRepresentation()));
        if (series.sourceInternalProducerId() != null) {
            assignment = SourceAssignment.internal(new InternalProducerId(series.sourceInternalProducerId()));
        }

        return new TimeSeries(
                new TimeSeriesId(series.id()),
                new MeasuringPointId(series.measuringPointId()),
                new ObservedPropertyCode(series.observedProperty()),
                MetadataStatus.from(series.status()),
                assignment);
    }
}
