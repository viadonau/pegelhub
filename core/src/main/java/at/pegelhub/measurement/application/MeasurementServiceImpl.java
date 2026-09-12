package at.pegelhub.measurement.application;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.measurement.domain.MeasurementBucket;
import at.pegelhub.measurement.domain.MeasurementConversion;
import at.pegelhub.measurement.domain.WriteMeasurement;
import at.pegelhub.measurement.domain.WriteMeasurements;
import at.pegelhub.measurement.persistence.MeasurementRepository;
import at.pegelhub.measuringpoint.application.MeasuringPointService;
import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.station.application.StationService;
import at.pegelhub.station.domain.Station;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.timeseries.application.TimeSeriesService;
import at.pegelhub.timeseries.domain.MeasurementRepresentation;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Default implementation for {@code MeasurementService}.
 */
@Service
public class MeasurementServiceImpl implements MeasurementService {

    private final MeasurementRepository measurementRepository;
    private final MeasurementAuthorizationPolicy authorizationPolicy;
    private final Clock clock;
    private final TimeSeriesService timeSeries;
    private final MeasuringPointService measuringPoints;
    private final StationService stations;

    public MeasurementServiceImpl(
            MeasurementRepository measurementRepository,
            MeasurementAuthorizationPolicy authorizationPolicy,
            Clock clock,
            TimeSeriesService timeSeries,
            MeasuringPointService measuringPoints,
            StationService stations) {
        this.measurementRepository = requireNonNull(measurementRepository);
        this.authorizationPolicy = requireNonNull(authorizationPolicy);
        this.clock = requireNonNull(clock);
        this.timeSeries = requireNonNull(timeSeries);
        this.measuringPoints = requireNonNull(measuringPoints);
        this.stations = requireNonNull(stations);
    }

    /**
     * processes the measurements to be saved to the time series database
     * @param writeMeasurements to save.
     */
    @Override
    public void writeMeasurements(WriteMeasurements writeMeasurements) {
        Instant receivedAt = Instant.now(clock);
        ConnectorId writer = authorizationPolicy.requireWriter();
        var targets = loadWriteTargets(writeMeasurements);
        targets.values().forEach(target -> authorizationPolicy.requireWrite(writer, target));
        var conversions = new HashMap<TimeSeriesId, MeasurementConversion>();
        targets.forEach((id, target) -> conversions.put(id, new MeasurementConversion(
                target.timeSeries().observedProperty(), target.timeSeries().sourceRepresentation(),
                target.measuringPoint().gaugeZeroElevationMAboveAdria())));
        List<Measurement> measurements = new ArrayList<>(writeMeasurements.measurements().size());
        for (WriteMeasurement measurement : writeMeasurements.measurements()) {
            var conversion = conversions.get(measurement.timeSeriesId());
            measurements.add(new Measurement(
                    measurement.timeSeriesId(),
                    measurement.observedAt(),
                    receivedAt,
                    conversion.toCanonical(measurement.value()),
                    writer));
        }
        measurementRepository.storeMeasurements(measurements);
    }

    private Map<TimeSeriesId, MeasurementWriteTarget> loadWriteTargets(WriteMeasurements write) {
        var targets = new LinkedHashMap<TimeSeriesId, MeasurementWriteTarget>();
        var points = new HashMap<MeasuringPointId, MeasuringPoint>();
        var stationMetadata = new HashMap<StationId, Station>();
        for (TimeSeriesId id : write.measurements().stream().map(WriteMeasurement::timeSeriesId).distinct().toList()) {
            var series = timeSeries.get(id);
            var point = points.computeIfAbsent(series.measuringPointId(), measuringPoints::get);
            var station = stationMetadata.computeIfAbsent(point.stationId(), stations::get);
            targets.put(id, new MeasurementWriteTarget(series, point, station));
        }
        return targets;
    }

    @Override
    public MeasurementList listMeasurements(MeasurementListQuery query) {
        requireNonNull(query);
        authorizationPolicy.requireRead(query.timeSeriesId());
        var conversion = outputConversion(query.timeSeriesId(), query.representation());
        var page = measurementRepository.listMeasurements(query);
        return new MeasurementList(query, page.truncated(), page.measurements().stream()
                .map(row -> new MeasurementReadRow(row.observedAt(),
                        conversion.fromCanonical(row.value()), row.submittedByConnectorId()))
                .toList(), conversion.unit());
    }

    @Override
    public MeasurementBucketList listMeasurementBuckets(MeasurementBucketQuery query) {
        requireNonNull(query);
        authorizationPolicy.requireRead(query.timeSeriesId());
        var conversion = outputConversion(query.timeSeriesId(), query.representation());
        return new MeasurementBucketList(query, measurementRepository.listMeasurementBuckets(query).stream()
                .map(bucket -> new MeasurementBucket(bucket.timeSeriesId(), bucket.from(), bucket.to(),
                        conversion.fromCanonical(bucket.value()), bucket.sampleCount()))
                .toList(), conversion.unit());
    }

    private MeasurementConversion outputConversion(TimeSeriesId id, MeasurementRepresentation representation) {
        var series = timeSeries.get(id);
        var gaugeZero = representation == MeasurementRepresentation.METRES_ABOVE_ADRIA
                ? measuringPoints.get(series.measuringPointId()).gaugeZeroElevationMAboveAdria() : null;
        return new MeasurementConversion(series.observedProperty(), representation, gaugeZero);
    }

    @Override
    public List<LatestMeasurement> listLatestMeasurements(MeasurementLatestQuery query) {
        requireNonNull(query);
        if (query.timeSeriesIds().isEmpty()) {
            return List.of();
        }
        authorizationPolicy.requireReadBatch(query.timeSeriesIds());
        return measurementRepository.listLatestMeasurements(query);
    }

    @Override
    public Instant getSystemTime() {
        return measurementRepository.getSystemTime();
    }

}
