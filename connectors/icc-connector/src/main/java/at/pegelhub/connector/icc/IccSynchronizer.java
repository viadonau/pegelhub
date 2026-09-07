package at.pegelhub.connector.icc;

import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.config.WindowedPollingConfig;
import at.pegelhub.lib.exception.NotFoundException;
import at.pegelhub.lib.model.Measurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Copies recent time-series measurements between the local and remote Core instances. */
public class IccSynchronizer implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(IccSynchronizer.class);
    private final PegelHubClient coreClient;
    private final PegelHubClient externalClient;
    private final List<IccMapping> mappings;
    private final Duration initialLookback;
    private final Duration overlap;
    private final Clock clock;
    private final Map<IccMapping, Instant> nextSyncFrom = new HashMap<>();

    /** Uses a one-hour replay overlap in addition to the initial lookback. */
    public IccSynchronizer(
            PegelHubClient coreClient,
            PegelHubClient externalClient,
            List<IccMapping> mappings,
            Duration initialLookback) {
        this(coreClient, externalClient, mappings, initialLookback,
                WindowedPollingConfig.DEFAULT_OVERLAP, Clock.systemUTC());
    }

    public IccSynchronizer(
            PegelHubClient coreClient,
            PegelHubClient externalClient,
            List<IccMapping> mappings,
            Duration initialLookback,
            Duration overlap) {
        this(coreClient, externalClient, mappings, initialLookback, overlap, Clock.systemUTC());
    }

    IccSynchronizer(
            PegelHubClient coreClient,
            PegelHubClient externalClient,
            List<IccMapping> mappings,
            Duration initialLookback,
            Clock clock) {
        this(coreClient, externalClient, mappings, initialLookback,
                WindowedPollingConfig.DEFAULT_OVERLAP, clock);
    }

    IccSynchronizer(
            PegelHubClient coreClient,
            PegelHubClient externalClient,
            List<IccMapping> mappings,
            Duration initialLookback,
            Duration overlap,
            Clock clock) {
        if (overlap.isNegative() || overlap.isZero()) {
            throw new IllegalArgumentException("overlap must be positive");
        }
        this.coreClient = coreClient;
        this.externalClient = externalClient;
        this.mappings = List.copyOf(mappings);
        this.initialLookback = initialLookback;
        this.overlap = overlap;
        this.clock = clock;
    }

    /** Copies each mapping's next explicit measurement window. */
    @Override
    public void run() {
        Instant cycleUntil = clock.instant();
        for (IccMapping mapping : mappings) {
            Instant from = nextSyncFrom.computeIfAbsent(
                    mapping,
                    ignored -> cycleUntil.minus(initialLookback).minus(overlap));
            boolean coreToExternal = mapping.direction() == MappingDirection.CORE_TO_EXTERNAL;
            PegelHubClient source = coreToExternal ? coreClient : externalClient;
            PegelHubClient target = coreToExternal ? externalClient : coreClient;
            UUID sourceTimeSeriesId = coreToExternal
                    ? mapping.timeSeriesId()
                    : mapping.externalTimeSeriesId();
            UUID targetTimeSeriesId = coreToExternal
                    ? mapping.externalTimeSeriesId()
                    : mapping.timeSeriesId();
            try {
                sync(source, target, sourceTimeSeriesId, targetTimeSeriesId, from, cycleUntil);
                Instant nextFrom = cycleUntil.minus(overlap);
                if (nextFrom.isAfter(from)) {
                    nextSyncFrom.put(mapping, nextFrom);
                }
            } catch (NotFoundException nfe) {
                LOG.error("Source TimeSeries {} was not found", sourceTimeSeriesId);
            } catch (Exception ex) {
                LOG.error("Error when syncing source TimeSeries {}", sourceTimeSeriesId, ex);
            }
        }
    }

    private void sync(
            PegelHubClient source,
            PegelHubClient target,
            UUID sourceTimeSeriesId,
            UUID targetTimeSeriesId,
            Instant from,
            Instant to) {
        if (!to.isAfter(from)) {
            return;
        }

        List<Measurement> measurements = source.getMeasurementsOfTimeSeries(sourceTimeSeriesId, from, to).stream()
                .map(measurement -> new Measurement(
                        targetTimeSeriesId,
                        measurement.getObservedAt(),
                        measurement.getValue()))
                .toList();
        if (!measurements.isEmpty()) {
            target.sendMeasurements(measurements);
        }
    }
}
