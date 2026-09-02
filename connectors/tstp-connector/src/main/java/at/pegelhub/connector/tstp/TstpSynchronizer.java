package at.pegelhub.connector.tstp;

import at.pegelhub.connector.tstp.catalog.TstpCatalogResolver;
import at.pegelhub.connector.tstp.client.TstpClient;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.model.Measurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class TstpSynchronizer implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(TstpSynchronizer.class);

    private final PegelHubClient coreClient;
    private final TstpClient tstpClient;
    private final TstpCatalogResolver catalogResolver;
    private final List<TstpMapping> mappings;
    private final Duration initialLookback;
    private final Clock clock;
    private final Map<TstpMapping, Instant> nextSyncFrom = new HashMap<>();

    TstpSynchronizer(
            PegelHubClient coreClient,
            TstpClient tstpClient,
            TstpCatalogResolver catalogResolver,
            List<TstpMapping> mappings,
            Duration initialLookback) {
        this(coreClient, tstpClient, catalogResolver, mappings, initialLookback, Clock.systemUTC());
    }

    TstpSynchronizer(
            PegelHubClient coreClient,
            TstpClient tstpClient,
            TstpCatalogResolver catalogResolver,
            List<TstpMapping> mappings,
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
        Instant cycleUntil = clock.instant().truncatedTo(ChronoUnit.SECONDS);

        for (TstpMapping mapping : mappings) {
            Instant from = nextSyncFrom.computeIfAbsent(
                    mapping,
                    ignored -> cycleUntil.minus(initialLookback));
            try {
                synchronizeMapping(mapping, from, cycleUntil);
                nextSyncFrom.put(mapping, cycleUntil);
            } catch (Exception e) {
                LOG.error("TSTP mapping failed: timeSeriesId={}, stationId={}, direction={}",
                        mapping.timeSeriesId(),
                        mapping.stationId(),
                        mapping.direction(),
                        e);
            }
        }
    }

    private void synchronizeMapping(TstpMapping mapping, Instant from, Instant until) {
        if (!until.isAfter(from)) {
            return;
        }

        String zrid = catalogResolver.resolveZrid(mapping.stationId());

        if (mapping.direction() == MappingDirection.EXTERNAL_TO_CORE) {
            List<Measurement> measurements = tstpClient.readMeasurements(zrid, from, until).stream()
                    .filter(measurement -> isInsideWindow(measurement, from, until))
                    .toList();

            if (measurements.isEmpty()) {
                return;
            }

            coreClient.sendMeasurements(measurements.stream()
                    .map(measurement -> withTimeSeriesId(measurement, mapping))
                    .toList());

            return;
        }

        List<Measurement> measurements = coreClient
                .getMeasurementsOfTimeSeries(mapping.timeSeriesId(), from, until)
                .stream()
                .filter(measurement -> isInsideWindow(measurement, from, until))
                .sorted(Comparator.comparing(Measurement::getObservedAt))
                .toList();

        if (measurements.isEmpty()) {
            return;
        }

        tstpClient.writeMeasurements(zrid, measurements);
    }

    private boolean isInsideWindow(
            Measurement measurement,
            Instant from,
            Instant until) {
        return !measurement.getObservedAt().isBefore(from)
                && measurement.getObservedAt().isBefore(until);
    }

    private Measurement withTimeSeriesId(Measurement measurement, TstpMapping mapping) {
        return new Measurement(mapping.timeSeriesId(), measurement.getObservedAt(), measurement.getValue());
    }
}
