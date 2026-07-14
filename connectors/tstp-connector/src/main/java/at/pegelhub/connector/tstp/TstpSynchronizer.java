package at.pegelhub.connector.tstp;

import at.pegelhub.connector.tstp.catalog.TstpCatalogResolver;
import at.pegelhub.connector.tstp.client.TstpClient;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.model.Measurement;
import at.pegelhub.lib.runtime.LoadedMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class TstpSynchronizer implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(TstpSynchronizer.class);

    private final PegelHubClient coreClient;
    private final TstpClient tstpClient;
    private final TstpCatalogResolver catalogResolver;
    private final List<LoadedMapping<TstpMapping>> mappings;
    private final Duration lookback;

    TstpSynchronizer(
            PegelHubClient coreClient,
            TstpClient tstpClient,
            TstpCatalogResolver catalogResolver,
            List<LoadedMapping<TstpMapping>> mappings,
            Duration lookback) {
        this.coreClient = coreClient;
        this.tstpClient = tstpClient;
        this.catalogResolver = catalogResolver;
        this.mappings = List.copyOf(mappings);
        this.lookback = lookback;
    }

    @Override
    public void run() {
        int succeeded = 0;
        int skipped = 0;
        List<MappingFailure> failures = new ArrayList<>();

        for (LoadedMapping<TstpMapping> loaded : mappings) {
            try {
                boolean changed = synchronize(loaded.value());
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

    private boolean synchronize(TstpMapping mapping) {
        String zrid = catalogResolver.resolveZrid(mapping.stationId());
        if (mapping.direction() == MappingDirection.EXTERNAL_TO_CORE) {
            Instant until = Instant.now();
            List<Measurement> measurements = tstpClient.readMeasurements(zrid, until.minus(lookback), until);
            if (measurements.isEmpty()) {
                return false;
            }
            coreClient.sendMeasurements(measurements.stream()
                    .map(measurement -> withTimeSeriesId(measurement, mapping))
                    .toList());
            return true;
        }

        List<Measurement> measurements = coreClient
                .getMeasurementsOfTimeSeries(mapping.timeSeriesId(), lookback)
                .stream()
                .sorted(Comparator.comparing(Measurement::getObservedAt))
                .toList();
        if (measurements.isEmpty()) {
            return false;
        }
        tstpClient.writeMeasurements(zrid, measurements);
        if (mapping.verifyRoundTrip()) {
            verifyRoundTrip(zrid, measurements);
        }
        return true;
    }

    private void verifyRoundTrip(String zrid, List<Measurement> expected) {
        Instant from = expected.getFirst().getObservedAt().truncatedTo(ChronoUnit.SECONDS);
        Instant until = expected.getLast().getObservedAt().truncatedTo(ChronoUnit.SECONDS).plusSeconds(1);

        // Real-system integration will determine whether this needs bounded polling for eventual visibility.
        List<Measurement> actual = tstpClient.readMeasurements(zrid, from, until);
        for (Measurement measurement : expected) {
            NormalizedMeasurement normalized = normalize(measurement);
            boolean present = actual.stream().map(this::normalize).anyMatch(normalized::equals);
            if (!present) {
                throw new IllegalStateException(
                        "TSTP round-trip verification did not return measurement " + normalized);
            }
        }
        LOG.info("TSTP round-trip verification passed for ZRID {} with {} measurement(s)", zrid, expected.size());
    }

    private Measurement withTimeSeriesId(Measurement measurement, TstpMapping mapping) {
        return new Measurement(mapping.timeSeriesId(), measurement.getObservedAt(), measurement.getValue());
    }

    private NormalizedMeasurement normalize(Measurement measurement) {
        double value = BigDecimal.valueOf(measurement.getValue().floatValue())
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
        return new NormalizedMeasurement(
                measurement.getObservedAt().truncatedTo(ChronoUnit.SECONDS),
                value);
    }

    private record NormalizedMeasurement(Instant observedAt, double value) {
    }

    private record MappingFailure(String fileName, Exception cause) {
    }
}
