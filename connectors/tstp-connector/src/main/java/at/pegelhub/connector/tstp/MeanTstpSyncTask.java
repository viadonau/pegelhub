package at.pegelhub.connector.tstp;

import at.pegelhub.connector.tstp.catalog.TstpCatalogResolver;
import at.pegelhub.connector.tstp.client.TstpClient;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.model.Measurement;
import at.pegelhub.lib.model.MeasurementIntervalStatistics;
import at.pegelhub.lib.model.MeasurementRepresentation;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

final class MeanTstpSyncTask implements TstpSyncTask {
    // Publication batches remain smaller than Core's query limit to bound each PUT and retry.
    private static final int MAX_INTERVALS_PER_REQUEST = 1_000;

    private final PegelHubClient coreClient;
    private final TstpClient tstpClient;
    private final TstpCatalogResolver catalogResolver;
    private final TstpMapping mapping;
    private final TstpMeanExport export;
    private final Duration initialLookback;
    private final Duration overlap;

    private Instant replayFrom;
    private Instant publishedUntil;

    MeanTstpSyncTask(
            PegelHubClient coreClient,
            TstpClient tstpClient,
            TstpCatalogResolver catalogResolver,
            TstpMapping mapping,
            Duration initialLookback,
            Duration overlap) {
        this.coreClient = coreClient;
        this.tstpClient = tstpClient;
        this.catalogResolver = catalogResolver;
        this.mapping = mapping;
        this.export = mapping.meanExport();
        this.initialLookback = initialLookback;
        this.overlap = overlap;
    }

    @Override
    public TstpMapping mapping() {
        return mapping;
    }

    @Override
    public void synchronize(Instant cycleUntil) {
        IntervalWindow window = nextWindow(cycleUntil);
        if (window == null) {
            return;
        }

        String zrid = catalogResolver.resolveZrid(mapping.stationId(), mapping.parameter(), mapping.unit());
        MeasurementRepresentation representation = mapping.parameter().representation(mapping.unit());
        MeasurementIntervalStatistics statistics = coreClient.getMeasurementIntervals(
                mapping.timeSeriesId(), window.from(), window.to(), export.interval(), export.timeBasis(),
                true, representation);
        requirePublishable(window, representation, statistics);

        tstpClient.writeMeasurements(zrid, endLabeledMeans(statistics), mapping.unit());
        replayFrom = export.alignedFloor(window.to().minus(overlap));
        publishedUntil = window.to();
    }

    private IntervalWindow nextWindow(Instant cycleUntil) {
        Duration width = export.width();
        Instant eligibleUntil = export.alignedFloor(cycleUntil.minus(export.settlingDuration()));
        if (replayFrom == null) {
            replayFrom = export.alignedFloor(eligibleUntil.minus(initialLookback).minus(overlap));
            publishedUntil = replayFrom;
        }
        // Short polls do no work until another aligned interval has closed.
        if (!eligibleUntil.isAfter(publishedUntil)) {
            return null;
        }

        Instant batchLimit = replayFrom.plus(width.multipliedBy(MAX_INTERVALS_PER_REQUEST));
        Instant to = eligibleUntil.isBefore(batchLimit) ? eligibleUntil : batchLimit;
        return to.isAfter(replayFrom) ? new IntervalWindow(replayFrom, to) : null;
    }

    private void requirePublishable(
            IntervalWindow window,
            MeasurementRepresentation representation,
            MeasurementIntervalStatistics statistics) {
        statistics.requireMatches(mapping.timeSeriesId(), window.from(), window.to(), export.interval(),
                export.timeBasis(), true, representation);
        if (statistics.intervals().isEmpty()
                || !statistics.intervals().getLast().to().equals(window.to())) {
            throw new IllegalStateException("Core has not closed every requested interval");
        }
        if (!mapping.parameter().coreUnit(mapping.unit()).equals(statistics.unit())) {
            throw new IllegalStateException("Core returned an unexpected unit for the TSTP target");
        }
    }

    private static List<Measurement> endLabeledMeans(MeasurementIntervalStatistics statistics) {
        return statistics.intervals().stream().map(interval -> {
            Measurement measurement = new Measurement();
            measurement.setObservedAt(interval.to());
            // Null is translated to TSTP's official gap marker by the client.
            measurement.setValue(interval.mean());
            return measurement;
        }).toList();
    }

    private record IntervalWindow(Instant from, Instant to) { }
}
