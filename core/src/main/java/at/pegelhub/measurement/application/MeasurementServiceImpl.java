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
import java.util.Collection;
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
     * Processes the measurements to be saved to the time series database.
     *
     * @param writeMeasurements measurements to save
     */
    @Override
    public void writeMeasurements(WriteMeasurements writeMeasurements) {
        requireNonNull(writeMeasurements);

        Instant receivedAt = Instant.now(clock);
        ConnectorId writer = authorizationPolicy.requireWriter();
        Map<TimeSeriesId, MeasurementWriteTarget> targets = loadWriteTargets(writeMeasurements);
        authorizeWriteTargets(writer, targets.values());

        Map<TimeSeriesId, MeasurementConversion> conversions = createConversions(targets);
        List<Measurement> measurements = convertMeasurements(
                writeMeasurements, receivedAt, writer, conversions);

        measurementRepository.storeMeasurements(measurements);
    }

    private void authorizeWriteTargets(
            ConnectorId writer, Collection<MeasurementWriteTarget> targets) {
        for (MeasurementWriteTarget target : targets) {
            authorizationPolicy.requireWrite(writer, target);
        }
    }

    private Map<TimeSeriesId, MeasurementConversion> createConversions(
            Map<TimeSeriesId, MeasurementWriteTarget> targets) {
        Map<TimeSeriesId, MeasurementConversion> conversions = new HashMap<>();
        for (Map.Entry<TimeSeriesId, MeasurementWriteTarget> entry : targets.entrySet()) {
            MeasurementWriteTarget target = entry.getValue();
            conversions.put(entry.getKey(), new MeasurementConversion(
                    target.timeSeries().observedProperty(),
                    target.timeSeries().sourceRepresentation(),
                    target.measuringPoint().gaugeZeroElevationMAboveAdria()));
        }
        return conversions;
    }

    private List<Measurement> convertMeasurements(
            WriteMeasurements writeMeasurements,
            Instant receivedAt,
            ConnectorId writer,
            Map<TimeSeriesId, MeasurementConversion> conversions) {
        List<Measurement> measurements = new ArrayList<>(writeMeasurements.measurements().size());
        for (WriteMeasurement measurement : writeMeasurements.measurements()) {
            MeasurementConversion conversion = conversions.get(measurement.timeSeriesId());
            measurements.add(new Measurement(
                    measurement.timeSeriesId(),
                    measurement.observedAt(),
                    receivedAt,
                    conversion.toCanonical(measurement.value()),
                    writer));
        }
        return measurements;
    }

    private Map<TimeSeriesId, MeasurementWriteTarget> loadWriteTargets(WriteMeasurements write) {
        var targets = new LinkedHashMap<TimeSeriesId, MeasurementWriteTarget>();
        var points = new HashMap<MeasuringPointId, MeasuringPoint>();
        var stationMetadata = new HashMap<StationId, Station>();
        List<TimeSeriesId> ids = write.measurements().stream()
                .map(WriteMeasurement::timeSeriesId)
                .distinct()
                .toList();
        for (TimeSeriesId id : ids) {
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
        List<MeasurementReadRow> measurements = page.measurements().stream()
                .map(row -> toReadRow(row, conversion))
                .toList();
        return new MeasurementList(query, page.truncated(), measurements, conversion.unit());
    }

    @Override
    public MeasurementBucketList listMeasurementBuckets(MeasurementBucketQuery query) {
        requireNonNull(query);
        authorizationPolicy.requireRead(query.timeSeriesId());
        var conversion = outputConversion(query.timeSeriesId(), query.representation());
        List<MeasurementBucket> buckets = measurementRepository.listMeasurementBuckets(query).stream()
                .map(bucket -> toBucket(bucket, conversion))
                .toList();
        return new MeasurementBucketList(query, buckets, conversion.unit());
    }

    private MeasurementReadRow toReadRow(
            MeasurementReadRow row, MeasurementConversion conversion) {
        return new MeasurementReadRow(
                row.observedAt(),
                conversion.fromCanonical(row.value()),
                row.submittedByConnectorId());
    }

    private MeasurementBucket toBucket(
            MeasurementBucket bucket, MeasurementConversion conversion) {
        return new MeasurementBucket(
                bucket.timeSeriesId(),
                bucket.from(),
                bucket.to(),
                conversion.fromCanonical(bucket.value()),
                bucket.sampleCount());
    }

    private MeasurementConversion outputConversion(
            TimeSeriesId id, MeasurementRepresentation representation) {
        var series = timeSeries.get(id);
        var gaugeZero = representation == MeasurementRepresentation.METRES_ABOVE_ADRIA
                ? measuringPoints.get(series.measuringPointId()).gaugeZeroElevationMAboveAdria()
                : null;
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
