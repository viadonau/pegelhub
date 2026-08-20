package at.pegelhub.timeseries.application;

import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.connector.persistence.ConnectorRepository;
import at.pegelhub.measuringpoint.application.MeasuringPointService;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.shared.error.MetadataConflictException;
import at.pegelhub.shared.metadata.MetadataStatus;
import at.pegelhub.station.application.StationService;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.timeseries.domain.SourceAssignment;
import at.pegelhub.timeseries.domain.SourceRepresentation;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import at.pegelhub.timeseries.persistence.TimeSeriesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.Objects.requireNonNull;

@Service
class TimeSeriesServiceImpl implements TimeSeriesService {
    private final TimeSeriesRepository timeSeries;
    private final MeasuringPointService measuringPoints;
    private final StationService stations;
    private final ConnectorRepository connectors;

    TimeSeriesServiceImpl(TimeSeriesRepository timeSeries, MeasuringPointService measuringPoints,
                          StationService stations, ConnectorRepository connectors) {
        this.timeSeries = requireNonNull(timeSeries);
        this.measuringPoints = requireNonNull(measuringPoints);
        this.stations = requireNonNull(stations);
        this.connectors = requireNonNull(connectors);
    }

    @Override
    @Transactional
    public TimeSeries create(CreateTimeSeriesCommand command) {
        requireNonNull(command);
        measuringPoints.getForUpdate(command.measuringPointId());
        validateSource(command.measuringPointId(), command.sourceAssignment());
        return timeSeries.save(TimeSeries.create(
                command.measuringPointId(), command.observedProperty(), command.status(), command.sourceAssignment()));
    }

    @Override
    @Transactional
    public TimeSeries update(TimeSeriesId id, UpdateTimeSeriesCommand command) {
        requireNonNull(command);
        TimeSeries existing = get(id);
        measuringPoints.getForUpdate(existing.measuringPointId());
        validateSource(existing.measuringPointId(), command.sourceAssignment());
        return timeSeries.save(existing.update(command.status(), command.sourceAssignment()));
    }

    @Override
    public TimeSeries get(TimeSeriesId id) {
        requireNonNull(id);
        return timeSeries.findById(id).orElseThrow(() -> new NotFoundException("Time series not found: " + id.value()));
    }

    @Override public List<TimeSeries> list() { return timeSeries.findAll(); }

    @Override
    public List<TimeSeries> listForMeasuringPoint(MeasuringPointId id) {
        measuringPoints.get(id);
        return timeSeries.findByMeasuringPointId(id);
    }

    @Override
    public List<TimeSeries> listForStation(StationId id) {
        stations.get(id);
        return timeSeries.findByStationId(id);
    }

    private void validateSource(MeasuringPointId measuringPointId, SourceAssignment assignment) {
        if (assignment == null) return;
        if (assignment.representation() == SourceRepresentation.METRES_ABOVE_ADRIA) {
            var point = measuringPoints.get(measuringPointId);
            if (point.gaugeZeroElevationMAboveAdria() == null) {
                throw new IllegalArgumentException("Absolute water-level source requires gauge zero elevation");
            }
        }
        Connector connector = connectors.findById(assignment.connectorId())
                .orElseThrow(() -> new NotFoundException("Connector not found: " + assignment.connectorId().value()));
        if (connector.status() != MetadataStatus.ACTIVE) {
            throw new MetadataConflictException("Source connector must be active");
        }
    }

}
