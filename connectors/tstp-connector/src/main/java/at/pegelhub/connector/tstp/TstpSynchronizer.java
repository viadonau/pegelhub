package at.pegelhub.connector.tstp;

import at.pegelhub.connector.tstp.catalog.TstpCatalogResolver;
import at.pegelhub.connector.tstp.client.TstpClient;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.ConfigValidation;
import at.pegelhub.lib.config.WindowedPollingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

final class TstpSynchronizer implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(TstpSynchronizer.class);

    private final List<TstpSyncTask> tasks;
    private final Clock clock;

    TstpSynchronizer(
            PegelHubClient coreClient,
            TstpClient tstpClient,
            TstpCatalogResolver catalogResolver,
            List<TstpMapping> mappings,
            Duration initialLookback,
            Duration overlap) {
        this(coreClient, tstpClient, catalogResolver, mappings, initialLookback, overlap, Clock.systemUTC());
    }

    TstpSynchronizer(
            PegelHubClient coreClient,
            TstpClient tstpClient,
            TstpCatalogResolver catalogResolver,
            List<TstpMapping> mappings,
            Duration initialLookback,
            Clock clock) {
        this(coreClient, tstpClient, catalogResolver, mappings, initialLookback,
                WindowedPollingConfig.DEFAULT_OVERLAP, clock);
    }

    TstpSynchronizer(
            PegelHubClient coreClient,
            TstpClient tstpClient,
            TstpCatalogResolver catalogResolver,
            List<TstpMapping> mappings,
            Duration initialLookback,
            Duration overlap,
            Clock clock) {
        ConfigValidation.requirePositive(overlap, "overlap");
        this.tasks = mappings.stream()
                .map(mapping -> createTask(
                        coreClient, tstpClient, catalogResolver, mapping, initialLookback, overlap))
                .toList();
        this.clock = clock;
    }

    @Override
    public void run() {
        Instant cycleUntil = clock.instant().truncatedTo(ChronoUnit.SECONDS);

        for (TstpSyncTask task : tasks) {
            try {
                task.synchronize(cycleUntil);
            } catch (Exception e) {
                TstpMapping mapping = task.mapping();
                LOG.error("TSTP mapping failed: timeSeriesId={}, stationId={}, parameter={}, direction={}",
                        mapping.timeSeriesId(),
                        mapping.stationId(),
                        mapping.parameter().value(),
                        mapping.direction(),
                        e);
            }
        }
    }

    private static TstpSyncTask createTask(
            PegelHubClient coreClient,
            TstpClient tstpClient,
            TstpCatalogResolver catalogResolver,
            TstpMapping mapping,
            Duration initialLookback,
            Duration overlap) {
        if (mapping.meanExport() == null) {
            return new RawTstpSyncTask(
                    coreClient, tstpClient, catalogResolver, mapping, initialLookback, overlap);
        }
        return new MeanTstpSyncTask(
                coreClient, tstpClient, catalogResolver, mapping, initialLookback, overlap);
    }
}
