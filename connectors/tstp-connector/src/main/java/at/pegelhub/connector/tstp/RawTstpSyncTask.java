package at.pegelhub.connector.tstp;

import at.pegelhub.connector.tstp.catalog.TstpCatalogResolver;
import at.pegelhub.connector.tstp.client.TstpClient;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.model.Measurement;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

final class RawTstpSyncTask implements TstpSyncTask {
    private final PegelHubClient coreClient;
    private final TstpClient tstpClient;
    private final TstpCatalogResolver catalogResolver;
    private final TstpMapping mapping;
    private final Duration initialLookback;
    private final Duration overlap;

    private Instant replayFrom;

    RawTstpSyncTask(
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
        this.initialLookback = initialLookback;
        this.overlap = overlap;
    }

    @Override
    public TstpMapping mapping() {
        return mapping;
    }

    @Override
    public void synchronize(Instant cycleUntil) {
        if (replayFrom == null) {
            replayFrom = cycleUntil.minus(initialLookback).minus(overlap);
        }
        if (!cycleUntil.isAfter(replayFrom)) {
            return;
        }

        transfer(replayFrom, cycleUntil);
        Instant nextReplayFrom = cycleUntil.minus(overlap);
        if (nextReplayFrom.isAfter(replayFrom)) {
            replayFrom = nextReplayFrom;
        }
    }

    private void transfer(Instant from, Instant until) {
        String zrid = catalogResolver.resolveZrid(mapping.stationId(), mapping.parameter(), mapping.unit());
        if (mapping.direction() == MappingDirection.EXTERNAL_TO_CORE) {
            importMeasurements(zrid, from, until);
        } else {
            exportMeasurements(zrid, from, until);
        }
    }

    private void importMeasurements(String zrid, Instant from, Instant until) {
        List<Measurement> measurements = tstpClient.readMeasurements(zrid, from, until, mapping.unit()).stream()
                .filter(measurement -> isInsideWindow(measurement, from, until))
                .map(this::withTimeSeriesId)
                .toList();
        if (!measurements.isEmpty()) {
            coreClient.sendMeasurements(measurements);
        }
    }

    private void exportMeasurements(String zrid, Instant from, Instant until) {
        List<Measurement> measurements = coreClient
                .getMeasurementsOfTimeSeries(mapping.timeSeriesId(), from, until,
                        mapping.parameter().representation(mapping.unit()))
                .stream()
                .filter(measurement -> isInsideWindow(measurement, from, until))
                .sorted(Comparator.comparing(Measurement::getObservedAt))
                .toList();
        if (!measurements.isEmpty()) {
            // PUT replaces the covered span, so replay neighbors as well as late arrivals.
            tstpClient.writeMeasurements(zrid, measurements, mapping.unit());
        }
    }

    private static boolean isInsideWindow(Measurement measurement, Instant from, Instant until) {
        return !measurement.getObservedAt().isBefore(from)
                && measurement.getObservedAt().isBefore(until);
    }

    private Measurement withTimeSeriesId(Measurement measurement) {
        return new Measurement(mapping.timeSeriesId(), measurement.getObservedAt(), measurement.getValue());
    }
}
