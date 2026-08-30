package at.pegelhub.connector.icc;

import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.MappingDirection;
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
    private final Clock clock;
    private final Map<IccMapping, Instant> nextSyncFrom = new HashMap<>();

    public IccSynchronizer(
            PegelHubClient coreClient,
            PegelHubClient externalClient,
            List<IccMapping> mappings,
            Duration initialLookback) {
        this(coreClient, externalClient, mappings, initialLookback, Clock.systemUTC());
    }

    IccSynchronizer(
            PegelHubClient coreClient,
            PegelHubClient externalClient,
            List<IccMapping> mappings,
            Duration initialLookback,
            Clock clock) {
        this.coreClient = coreClient;
        this.externalClient = externalClient;
        this.mappings = List.copyOf(mappings);
        this.initialLookback = initialLookback;
        this.clock = clock;
    }

    /** Copies each mapping's next explicit measurement window. */
    @Override
    public void run() {
        Instant cycleUntil = clock.instant();
        for (IccMapping mapping : mappings) {
            Instant from = nextSyncFrom.computeIfAbsent(
                    mapping,
                    ignored -> cycleUntil.minus(initialLookback));
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
                nextSyncFrom.put(mapping, cycleUntil);
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
