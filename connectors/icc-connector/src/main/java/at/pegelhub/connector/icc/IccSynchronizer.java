package at.pegelhub.connector.icc;

import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.model.Measurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Serial polling job; each directed transfer owns its in-memory retry boundary. */
final class IccSynchronizer implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(IccSynchronizer.class);

    private final List<Transfer> transfers;
    private final Duration initialWindow;
    private final Duration overlap;
    private final Clock clock;

    IccSynchronizer(
            PegelHubClient localCore,
            PegelHubClient remoteCore,
            List<IccMapping> mappings,
            Duration pollInterval,
            Duration overlap) {
        this(localCore, remoteCore, mappings, pollInterval, overlap, Clock.systemUTC());
    }

    IccSynchronizer(
            PegelHubClient localCore,
            PegelHubClient remoteCore,
            List<IccMapping> mappings,
            Duration pollInterval,
            Duration overlap,
            Clock clock) {
        Objects.requireNonNull(localCore, "localCore");
        Objects.requireNonNull(remoteCore, "remoteCore");
        if (pollInterval.isZero() || pollInterval.isNegative()) {
            throw new IllegalArgumentException("pollInterval must be positive");
        }
        if (overlap.isZero() || overlap.isNegative()) {
            throw new IllegalArgumentException("overlap must be positive");
        }
        this.initialWindow = pollInterval.plus(overlap);
        this.overlap = overlap;
        this.clock = Objects.requireNonNull(clock, "clock");
        this.transfers = mappings.stream().map(mapping -> switch (mapping.direction()) {
            case CORE_TO_EXTERNAL -> new Transfer(
                    localCore, mapping.timeSeriesId(), remoteCore, mapping.externalTimeSeriesId());
            case EXTERNAL_TO_CORE -> new Transfer(
                    remoteCore, mapping.externalTimeSeriesId(), localCore, mapping.timeSeriesId());
        }).toList();
    }

    @Override
    public void run() {
        Instant until = clock.instant();
        for (Transfer transfer : transfers) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            try {
                transfer.copyUntil(until);
            } catch (Exception e) {
                LOG.error("ICC transfer {} -> {} failed for [{}, {}); retaining retry window",
                        transfer.sourceTimeSeriesId, transfer.targetTimeSeriesId, transfer.nextFrom, until, e);
            }
        }
    }

    private final class Transfer {
        private final PegelHubClient source;
        private final UUID sourceTimeSeriesId;
        private final PegelHubClient target;
        private final UUID targetTimeSeriesId;
        private Instant nextFrom;

        private Transfer(PegelHubClient source, UUID sourceTimeSeriesId, PegelHubClient target, UUID targetTimeSeriesId) {
            this.source = source;
            this.sourceTimeSeriesId = sourceTimeSeriesId;
            this.target = target;
            this.targetTimeSeriesId = targetTimeSeriesId;
        }

        private void copyUntil(Instant until) {
            if (nextFrom == null) {
                nextFrom = until.minus(initialWindow);
            }
            if (!until.isAfter(nextFrom)) {
                return;
            }

            List<Measurement> measurements = source.getMeasurementsOfTimeSeries(sourceTimeSeriesId, nextFrom, until)
                    .stream()
                    .map(measurement -> new Measurement(
                            targetTimeSeriesId, measurement.getObservedAt(), measurement.getValue()))
                    .toList();
            if (!measurements.isEmpty()) {
                target.sendMeasurements(measurements);
            }

            // A failed read or write never reaches this checkpoint, including during replay.
            Instant replayFrom = until.minus(overlap);
            if (replayFrom.isAfter(nextFrom)) {
                nextFrom = replayFrom;
            }
        }
    }
}
