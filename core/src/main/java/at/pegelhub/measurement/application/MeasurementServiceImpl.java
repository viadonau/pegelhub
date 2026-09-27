package at.pegelhub.measurement.application;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measurement.domain.CanonicalConversion;
import at.pegelhub.measurement.domain.LitresPerSecondConversion;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.measurement.domain.MeasurementConversion;
import at.pegelhub.measurement.domain.MetresAboveAdriaConversion;
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
import at.pegelhub.timeseries.domain.ObservedPropertyCatalog;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Checks access and uses {@link MeasurementConversion} to convert incoming and outgoing values.
 */
@Service
public class MeasurementServiceImpl implements MeasurementService {
    private static final int MAX_INTERVAL_EVIDENCE_ROWS = 100_000;

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

    @Override
    public void writeMeasurements(WriteMeasurements writeMeasurements) {
        requireNonNull(writeMeasurements);

        Instant receivedAt = Instant.now(clock);
        ConnectorId writer = authorizationPolicy.requireWriter();
        var targets = loadWriteTargets(writeMeasurements);

        // Check access for the whole batch first, so conversion errors don't hide permission errors.
        for (MeasurementWriteTarget target : targets.values()) {
            authorizationPolicy.requireWrite(writer, target);
        }

        var conversions = new HashMap<TimeSeriesId, MeasurementConversion>();
        for (MeasurementWriteTarget target : targets.values()) {
            var series = target.timeSeries();
            conversions.put(series.id(), conversionFor(
                    series,
                    series.sourceRepresentation(),
                    target::measuringPoint));
        }

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

        measurementRepository.storeMeasurements(measurements);
    }

    @Override
    public MeasurementList listMeasurements(MeasurementListQuery query) {
        requireNonNull(query);
        authorizationPolicy.requireRead(query.timeSeriesId());
        var conversion = outputConversion(query.timeSeriesId(), query.representation());
        var page = measurementRepository.listMeasurements(query);

        List<MeasurementReadRow> measurements = page.measurements().stream()
                .map(row -> new MeasurementReadRow(
                        row.observedAt(),
                        conversion.fromCanonical(row.value()),
                        row.submittedByConnectorId()))
                .toList();
        return new MeasurementList(query, page.truncated(), measurements, conversion.unit());
    }

    @Override
    public MeasurementIntervalList listMeasurementIntervals(MeasurementIntervalQuery query) {
        requireNonNull(query);
        authorizationPolicy.requireRead(query.timeSeriesId());
        var conversion = outputConversion(query.timeSeriesId(), query.representation());
        Instant computedAt = Instant.now(clock);
        var intervals = calculateIntervalMeans(query, computedAt).stream()
                .map(interval -> convertInterval(interval, conversion))
                .toList();
        return new MeasurementIntervalList(query, computedAt, conversion.unit(), intervals);
    }

    private List<MeasurementInterval> calculateIntervalMeans(
            MeasurementIntervalQuery query, Instant computedAt) {
        int selected = selectedWindowCount(query, computedAt);
        if (selected == 0) return List.of();

        Instant selectedTo = query.from().plus(query.width().multipliedBy(selected));
        Instant evidenceTo = selectedTo.isBefore(computedAt) ? selectedTo : computedAt;
        List<MeasurementReadRow> evidence = evidenceTo.isAfter(query.from())
                ? measurementRepository.listIntervalEvidence(
                        query.timeSeriesId(), new MeasurementWindow(query.from(), evidenceTo, null),
                        MAX_INTERVAL_EVIDENCE_ROWS)
                : List.of();
        return StepIntervalCalculator.calculate(
                query.from(), query.width(), selected, computedAt, evidence);
    }

    private static int selectedWindowCount(MeasurementIntervalQuery query, Instant computedAt) {
        Duration width = query.width();
        int requested = Math.toIntExact(Duration.between(query.from(), query.to()).dividedBy(width));
        if (!query.closedOnly()) return requested;

        int selected = 0;
        Instant intervalEnd = query.from().plus(width);
        while (selected < requested && !intervalEnd.isAfter(computedAt)) {
            selected++;
            intervalEnd = intervalEnd.plus(width);
        }
        return selected;
    }

    private static MeasurementInterval convertInterval(MeasurementInterval interval, MeasurementConversion conversion) {
        return new MeasurementInterval(interval.from(), interval.to(),
                interval.mean() == null ? null : conversion.fromCanonical(interval.mean()),
                interval.observationCount(), interval.supportedNanos(), interval.lastContributingObservedAt(),
                interval.windowStatus(), interval.supportStatus());
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

    private MeasurementConversion outputConversion(
            TimeSeriesId id, MeasurementRepresentation representation) {
        var series = timeSeries.get(id);
        return conversionFor(series, representation, () -> measuringPoints.get(series.measuringPointId()));
    }

    private static MeasurementConversion conversionFor(
            TimeSeries series,
            MeasurementRepresentation representation,
            Supplier<MeasuringPoint> point) {
        requireNonNull(representation);
        if (!ObservedPropertyCatalog.allows(series.observedProperty().value(), representation)) {
            throw new IllegalArgumentException(
                    "Representation " + representation.value()
                            + " is not supported for " + series.observedProperty().value());
        }

        // Only absolute water levels need a measuring point; other reads don't need to load it.
        return switch (representation) {
            case CANONICAL -> new CanonicalConversion(series.unit());
            case LITRES_PER_SECOND -> new LitresPerSecondConversion();
            case METRES_ABOVE_ADRIA -> new MetresAboveAdriaConversion(
                    point.get().gaugeZeroElevationMAboveAdria());
        };
    }

}
