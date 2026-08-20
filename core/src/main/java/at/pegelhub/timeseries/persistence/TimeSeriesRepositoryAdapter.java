package at.pegelhub.timeseries.persistence;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;
import at.pegelhub.timeseries.domain.SourceAssignment;
import at.pegelhub.timeseries.domain.SourceRepresentation;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import at.pegelhub.shared.metadata.MetadataStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Repository
class TimeSeriesRepositoryAdapter implements TimeSeriesRepository {
    private final SpringDataTimeSeriesRepository timeSeries;

    TimeSeriesRepositoryAdapter(SpringDataTimeSeriesRepository timeSeries) { this.timeSeries = requireNonNull(timeSeries); }

    @Override public TimeSeries save(TimeSeries series) { return toDomain(timeSeries.save(toEntity(series))); }
    @Override public Optional<TimeSeries> findById(TimeSeriesId id) { return timeSeries.findById(id.value()).map(this::toDomain); }
    @Override public List<TimeSeries> findAll() { return timeSeries.findAll().stream().map(this::toDomain).toList(); }
    @Override public List<TimeSeries> findByMeasuringPointId(MeasuringPointId id) { return timeSeries.findByMeasuringPointId(id.value()).stream().map(this::toDomain).toList(); }
    @Override public List<TimeSeries> findByStationId(StationId id) { return timeSeries.findByStationId(id.value()).stream().map(this::toDomain).toList(); }
    @Override public boolean hasAbsoluteSourceFor(MeasuringPointId id) { return timeSeries.existsByMeasuringPointIdAndSourceRepresentation(id.value(), SourceRepresentation.METRES_ABOVE_ADRIA.value()); }

    private TimeSeriesEntity toEntity(TimeSeries series) {
        SourceAssignment assignment = series.sourceAssignment();
        return new TimeSeriesEntity(
                series.id().value(), series.measuringPointId().value(), series.observedProperty().value(),
                series.status().value(), assignment == null ? null : assignment.connectorId().value(),
                assignment == null ? null : assignment.representation().value());
    }

    private TimeSeries toDomain(TimeSeriesEntity series) {
        SourceAssignment assignment = series.sourceConnectorId() == null ? null : new SourceAssignment(
                new ConnectorId(series.sourceConnectorId()), SourceRepresentation.from(series.sourceRepresentation()));
        return new TimeSeries(
                new TimeSeriesId(series.id()), new MeasuringPointId(series.measuringPointId()),
                new ObservedPropertyCode(series.observedProperty()), MetadataStatus.from(series.status()), assignment);
    }
}
