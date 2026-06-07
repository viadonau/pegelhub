package at.pegelhub.timeseries.persistence;

import at.pegelhub.station.domain.StationId;
import at.pegelhub.timeseries.domain.ExternalTimeSeriesCode;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import at.pegelhub.timeseries.domain.UnitCode;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Repository
final class JpaTimeSeriesRepositoryAdapter implements TimeSeriesRepository {

    private final SpringDataTimeSeriesRepository timeSeries;

    JpaTimeSeriesRepositoryAdapter(SpringDataTimeSeriesRepository timeSeries) {
        this.timeSeries = requireNonNull(timeSeries);
    }

    @Override
    public TimeSeries save(TimeSeries timeSeries) {
        requireNonNull(timeSeries);
        return toDomain(this.timeSeries.save(toJpa(timeSeries)));
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
    public List<TimeSeries> findByStationId(StationId stationId) {
        requireNonNull(stationId);
        return timeSeries.findByStationId(stationId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    private JpaTimeSeries toJpa(TimeSeries timeSeries) {
        return new JpaTimeSeries(
                timeSeries.id().value(),
                timeSeries.stationId().value(),
                timeSeries.observedProperty().value(),
                timeSeries.unit().value(),
                timeSeries.referenceLevel(),
                toSeconds(timeSeries.expectedInterval()),
                toExternalCodeValue(timeSeries.externalCode()));
    }

    private TimeSeries toDomain(JpaTimeSeries timeSeries) {
        return new TimeSeries(
                new TimeSeriesId(timeSeries.id()),
                new StationId(timeSeries.stationId()),
                new ObservedPropertyCode(timeSeries.observedProperty()),
                new UnitCode(timeSeries.unit()),
                timeSeries.referenceLevel(),
                toDuration(timeSeries.expectedIntervalSeconds()),
                toExternalCode(timeSeries.externalCode()));
    }

    private Long toSeconds(Duration duration) {
        return duration == null ? null : duration.toSeconds();
    }

    private Duration toDuration(Long seconds) {
        return seconds == null ? null : Duration.ofSeconds(seconds);
    }

    private String toExternalCodeValue(ExternalTimeSeriesCode externalCode) {
        return externalCode == null ? null : externalCode.value();
    }

    private ExternalTimeSeriesCode toExternalCode(String externalCode) {
        return externalCode == null ? null : new ExternalTimeSeriesCode(externalCode);
    }
}
