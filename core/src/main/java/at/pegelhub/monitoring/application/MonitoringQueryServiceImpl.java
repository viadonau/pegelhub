package at.pegelhub.monitoring.application;

import at.pegelhub.measuringpoint.application.MeasuringPointService;
import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.measurement.application.LatestMeasurement;
import at.pegelhub.measurement.application.MeasurementLatestQuery;
import at.pegelhub.measurement.application.MeasurementService;
import at.pegelhub.measurement.application.MeasurementWindow;
import at.pegelhub.station.application.StationService;
import at.pegelhub.station.domain.Station;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.stationowner.application.StationOwnerService;
import at.pegelhub.stationowner.domain.StationOwner;
import at.pegelhub.stationowner.domain.StationOwnerId;
import at.pegelhub.timeseries.application.TimeSeriesService;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import at.pegelhub.shared.metadata.MetadataStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@Service
final class MonitoringQueryServiceImpl implements MonitoringQueryService {

    private final TimeSeriesService timeSeriesService;
    private final MeasuringPointService measuringPointService;
    private final StationService stationService;
    private final StationOwnerService stationOwnerService;
    private final MeasurementService measurementService;
    private final MonitoringAuthorizationPolicy authorizationPolicy;
    private final Clock clock;

    MonitoringQueryServiceImpl(
            TimeSeriesService timeSeriesService,
            MeasuringPointService measuringPointService,
            StationService stationService,
            StationOwnerService stationOwnerService,
            MeasurementService measurementService,
            MonitoringAuthorizationPolicy authorizationPolicy,
            Clock clock) {
        this.timeSeriesService = requireNonNull(timeSeriesService);
        this.measuringPointService = requireNonNull(measuringPointService);
        this.stationService = requireNonNull(stationService);
        this.stationOwnerService = requireNonNull(stationOwnerService);
        this.measurementService = requireNonNull(measurementService);
        this.authorizationPolicy = requireNonNull(authorizationPolicy);
        this.clock = requireNonNull(clock);
    }

    @Override
    public MonitoringCollection readCollection(Duration latestWithin) {
        authorizationPolicy.requireRead();
        MeasurementWindow window = resolveWindow(latestWithin);
        List<TimeSeries> timeSeries = timeSeriesService.list();
        if (timeSeries.isEmpty()) {
            return new MonitoringCollection(List.of());
        }

        Map<MeasuringPointId, MeasuringPoint> measuringPoints = measuringPointService.list().stream()
                .collect(java.util.stream.Collectors.toMap(MeasuringPoint::id, point -> point));
        Map<StationId, Station> stations = stationService.list().stream()
                .collect(java.util.stream.Collectors.toMap(Station::id, station -> station));
        List<MonitoringCollection.MonitoringTimeSeriesSummary> items = timeSeries.stream()
                .map(series -> {
                    MeasuringPoint point = requireMeasuringPoint(measuringPoints, series.measuringPointId());
                    Station station = requireStation(stations, point.stationId());
                    if (series.status() != MetadataStatus.ACTIVE
                            || point.status() != MetadataStatus.ACTIVE
                            || station.status() != MetadataStatus.ACTIVE) {
                        return null;
                    }
                    return new MonitoringCollection.MonitoringTimeSeriesSummary(
                            series,
                            point,
                            station,
                            null);
                })
                .filter(java.util.Objects::nonNull)
                .toList();
        if (items.isEmpty()) {
            return new MonitoringCollection(List.of());
        }
        Map<TimeSeriesId, LatestMeasurement> latest = latestMeasurements(
                items.stream().map(MonitoringCollection.MonitoringTimeSeriesSummary::timeSeries).toList(), window);
        items = items.stream()
                .map(item -> new MonitoringCollection.MonitoringTimeSeriesSummary(
                        item.timeSeries(), item.measuringPoint(), item.station(), latest.get(item.timeSeries().id())))
                .toList();
        return new MonitoringCollection(items);
    }

    @Override
    public MonitoringDetail readDetail(TimeSeriesId timeSeriesId, Duration latestWithin) {
        authorizationPolicy.requireRead();
        requireNonNull(timeSeriesId);
        MeasurementWindow window = resolveWindow(latestWithin);
        TimeSeries timeSeries = timeSeriesService.get(timeSeriesId);
        MeasuringPoint measuringPoint = requireRelationship(
                () -> measuringPointService.get(timeSeries.measuringPointId()),
                "measuring point");
        Station station = requireRelationship(
                () -> stationService.get(measuringPoint.stationId()),
                "station");
        StationOwner stationOwner = requireRelationship(
                () -> stationOwnerService.get(station.ownerId()),
                "station owner");
        Map<TimeSeriesId, LatestMeasurement> latest = latestMeasurements(List.of(timeSeries), window);
        MetadataStatus effectiveStatus = timeSeries.status() == MetadataStatus.ACTIVE
                && measuringPoint.status() == MetadataStatus.ACTIVE
                && station.status() == MetadataStatus.ACTIVE
                ? MetadataStatus.ACTIVE
                : MetadataStatus.INACTIVE;
        return new MonitoringDetail(
                timeSeries, measuringPoint, station, stationOwner, effectiveStatus, latest.get(timeSeries.id()));
    }

    private Map<TimeSeriesId, LatestMeasurement> latestMeasurements(
            List<TimeSeries> timeSeries,
            MeasurementWindow window) {
        List<LatestMeasurement> latest = measurementService.listLatestMeasurements(
                new MeasurementLatestQuery(
                        timeSeries.stream().map(TimeSeries::id).toList(),
                        window));
        Map<TimeSeriesId, LatestMeasurement> result = new HashMap<>();
        for (LatestMeasurement measurement : latest) {
            result.put(measurement.timeSeriesId(), measurement);
        }
        return result;
    }

    private MeasurementWindow resolveWindow(Duration latestWithin) {
        requireNonNull(latestWithin);
        Instant to = Instant.now(clock);
        return new MeasurementWindow(to.minus(latestWithin), to, null);
    }

    private MeasuringPoint requireMeasuringPoint(Map<MeasuringPointId, MeasuringPoint> points, MeasuringPointId id) {
        MeasuringPoint point = points.get(id);
        if (point == null) {
            throw new IllegalStateException("TimeSeries refers to missing measuring point " + id.value());
        }
        return point;
    }

    private Station requireStation(Map<StationId, Station> stations, StationId id) {
        Station station = stations.get(id);
        if (station == null) {
            throw new IllegalStateException("Measuring point refers to missing station " + id.value());
        }
        return station;
    }

    private <T> T requireRelationship(java.util.function.Supplier<T> lookup, String relationship) {
        try {
            return lookup.get();
        } catch (at.pegelhub.shared.error.NotFoundException exception) {
            throw new IllegalStateException("Monitoring metadata consistency failure: missing " + relationship, exception);
        }
    }
}
