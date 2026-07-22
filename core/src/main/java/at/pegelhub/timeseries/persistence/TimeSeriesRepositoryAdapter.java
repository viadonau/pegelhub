package at.pegelhub.timeseries.persistence;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.timeseries.domain.ExternalTimeSeriesCode;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import at.pegelhub.timeseries.domain.UnitCode;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Repository
class TimeSeriesRepositoryAdapter implements TimeSeriesRepository {

    private final SpringDataTimeSeriesRepository timeSeries;

    TimeSeriesRepositoryAdapter(SpringDataTimeSeriesRepository timeSeries) {
        this.timeSeries = requireNonNull(timeSeries);
    }

    @Override
    public TimeSeries save(TimeSeries timeSeries) {
        requireNonNull(timeSeries);
        return toDomain(this.timeSeries.save(toEntity(timeSeries)));
    }

    @Override
    public Optional<TimeSeries> findById(TimeSeriesId id) {
        requireNonNull(id);
        return timeSeries.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<TimeSeries> findAll() {
        return timeSeries.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<TimeSeries> findByMeasuringPointId(MeasuringPointId measuringPointId) {
        requireNonNull(measuringPointId);
        return timeSeries.findByMeasuringPointId(measuringPointId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<TimeSeries> findByStationId(StationId stationId) {
        requireNonNull(stationId);
        return timeSeries.findByStationId(stationId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    private TimeSeriesEntity toEntity(TimeSeries timeSeries) {
        return new TimeSeriesEntity(
                timeSeries.id().value(),
                timeSeries.measuringPointId().value(),
                timeSeries.observedProperty().value(),
                timeSeries.unit().value(),
                toExternalCodeValue(timeSeries.externalCode()),
                toConnectorIdValue(timeSeries.sourceConnectorId()));
    }

    private TimeSeries toDomain(TimeSeriesEntity timeSeries) {
        return new TimeSeries(
                new TimeSeriesId(timeSeries.id()),
                new MeasuringPointId(timeSeries.measuringPointId()),
                new ObservedPropertyCode(timeSeries.observedProperty()),
                new UnitCode(timeSeries.unit()),
                toExternalCode(timeSeries.externalCode()),
                toConnectorId(timeSeries.sourceConnectorId()));
    }

    private String toExternalCodeValue(ExternalTimeSeriesCode externalCode) {
        return externalCode == null ? null : externalCode.value();
    }

    private ExternalTimeSeriesCode toExternalCode(String externalCode) {
        return externalCode == null ? null : new ExternalTimeSeriesCode(externalCode);
    }

    private java.util.UUID toConnectorIdValue(ConnectorId connectorId) {
        return connectorId == null ? null : connectorId.value();
    }

    private ConnectorId toConnectorId(java.util.UUID connectorId) {
        return connectorId == null ? null : new ConnectorId(connectorId);
    }
}
