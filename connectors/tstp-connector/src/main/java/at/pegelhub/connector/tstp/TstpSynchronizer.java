package at.pegelhub.connector.tstp;

import at.pegelhub.connector.tstp.catalog.TstpCatalogResolver;
import at.pegelhub.connector.tstp.client.TstpClient;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.model.Measurement;
import at.pegelhub.lib.runtime.LoadedMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class TstpSynchronizer implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(TstpSynchronizer.class);

    private final PegelHubClient coreClient;
    private final TstpClient tstpClient;
    private final TstpCatalogResolver catalogResolver;
    private final List<LoadedMapping<TstpMapping>> mappings;
    private final Duration initialLookback;
    private final Clock clock;
    private final Map<TstpMapping, Instant> synchronizedThrough = new HashMap<>();

    TstpSynchronizer(
            PegelHubClient coreClient,
            TstpClient tstpClient,
            TstpCatalogResolver catalogResolver,
            List<LoadedMapping<TstpMapping>> mappings,
            Duration initialLookback) {
        this(coreClient, tstpClient, catalogResolver, mappings, initialLookback, Clock.systemUTC());
    }

    TstpSynchronizer(
            PegelHubClient coreClient,
            TstpClient tstpClient,
            TstpCatalogResolver catalogResolver,
            List<LoadedMapping<TstpMapping>> mappings,
            Duration initialLookback,
            Clock clock) {
        this.coreClient = coreClient;
        this.tstpClient = tstpClient;
        this.catalogResolver = catalogResolver;
        this.mappings = List.copyOf(mappings);
        this.initialLookback = initialLookback;
        this.clock = clock;
    }

    @Override
    public void run() {
        int succeeded = 0;
        int skipped = 0;
        List<MappingFailure> failures = new ArrayList<>();

        for (LoadedMapping<TstpMapping> loaded : mappings) {
            Instant until = clock.instant().truncatedTo(ChronoUnit.SECONDS);
            try {
                boolean changed = synchronize(loaded.value(), until);
                synchronizedThrough.put(loaded.value(), until);
                if (changed) {
                    succeeded++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                failures.add(new MappingFailure(loaded.fileName(), e));
                LOG.error("TSTP mapping {} failed: timeSeriesId={}, stationId={}, direction={}",
                        loaded.fileName(),
                        loaded.value().timeSeriesId(),
                        loaded.value().stationId(),
                        loaded.value().direction(),
                        e);
            }
        }

        LOG.info("TSTP cycle completed: total={}, succeeded={}, skipped={}, failed={}",
                mappings.size(), succeeded, skipped, failures.size());
        if (!failures.isEmpty()) {
            IllegalStateException cycleFailure = new IllegalStateException(
                    "TSTP cycle failed for " + failures.size() + " of " + mappings.size() + " mappings");
            failures.forEach(failure -> cycleFailure.addSuppressed(failure.cause()));
            throw cycleFailure;
        }
    }

    private boolean synchronize(TstpMapping mapping, Instant until) {
        String zrid = catalogResolver.resolveZrid(mapping.stationId());
        Instant previousBoundary = synchronizedThrough.get(mapping);
        Instant requestedFrom = previousBoundary == null ? until.minus(initialLookback) : previousBoundary;
        Instant from = requestedFrom.isBefore(until) ? requestedFrom : until.minusSeconds(1);
        if (mapping.direction() == MappingDirection.EXTERNAL_TO_CORE) {
            List<Measurement> measurements = tstpClient.readMeasurements(zrid, from, until).stream()
                    .filter(measurement -> isInsideWindow(measurement, from, until, previousBoundary != null))
                    .toList();
            if (measurements.isEmpty()) {
                return false;
            }
            coreClient.sendMeasurements(measurements.stream()
                    .map(measurement -> withTimeSeriesId(measurement, mapping))
                    .toList());
            return true;
        }

        Duration coreLookback = Duration.between(from, clock.instant()).plusSeconds(1);
        List<Measurement> measurements = coreClient
                .getMeasurementsOfTimeSeries(mapping.timeSeriesId(), coreLookback)
                .stream()
                .filter(measurement -> isInsideWindow(measurement, from, until, previousBoundary != null))
                .sorted(Comparator.comparing(Measurement::getObservedAt))
                .toList();
        if (measurements.isEmpty()) {
            return false;
        }
        tstpClient.writeMeasurements(zrid, measurements);
        return true;
    }

    private boolean isInsideWindow(
            Measurement measurement,
            Instant from,
            Instant until,
            boolean continuing) {
        boolean afterStart = continuing
                ? measurement.getObservedAt().isAfter(from)
                : !measurement.getObservedAt().isBefore(from);
        return afterStart && !measurement.getObservedAt().isAfter(until);
    }

    private Measurement withTimeSeriesId(Measurement measurement, TstpMapping mapping) {
        return new Measurement(mapping.timeSeriesId(), measurement.getObservedAt(), measurement.getValue());
    }

    private record MappingFailure(String fileName, Exception cause) {
    }
}
