package at.pegelhub.quality.application;

import at.pegelhub.measurement.application.InternalMeasurements;
import at.pegelhub.measurement.application.MeasurementReadLimitException;
import at.pegelhub.measurement.application.MeasurementReadRow;
import at.pegelhub.measurement.application.MeasurementWindow;
import at.pegelhub.measurement.domain.InternalProducerId;
import at.pegelhub.notifications.application.Notifications;
import at.pegelhub.quality.domain.QualityEvaluator;
import at.pegelhub.quality.domain.QualityRun;
import at.pegelhub.quality.persistence.QualityRepository;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Owns the single QA execution slot in one Core process. Both scheduled methods share a
 * single-thread scheduler; fixed delay starts after completion, not at a wall-clock cadence.
 * This class must remain non-transactional: measurement I/O surrounds, but never joins,
 * the PostgreSQL transaction that records findings and queues notifications.
 */
@Component
public class QualityWorker {

    private static final Logger LOG = LoggerFactory.getLogger(QualityWorker.class);

    private enum Stage { READ, EVALUATE, RECORD, OUTPUT }

    private final QualityProperties properties;
    private final QualityRepository repository;
    private final Quality quality;
    private final InternalMeasurements measurements;
    private final Notifications notifications;
    private final Clock clock;

    private boolean recovered;

    public QualityWorker(
            QualityProperties properties,
            QualityRepository repository,
            Quality quality,
            InternalMeasurements measurements,
            Notifications notifications,
            Clock clock) {
        this.properties = properties;
        this.repository = repository;
        this.quality = quality;
        this.measurements = measurements;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 1000, scheduler = "qualityScheduler")
    public void tick() {
        try {
            executeNext();
        } catch (RuntimeException exception) {
            // A claim/release may have committed despite a lost response. Reconcile before claiming again.
            recovered = false;
            LOG.warn("QA lifecycle persistence failed: {}", exception.getClass().getSimpleName());
        }
    }

    private void executeNext() {
        if (!recovered) {
            repository.recover(clock.instant());
            recovered = true;
        }

        if (!properties.enabled()) {
            return;
        }

        var run = repository.claim(clock.instant());
        if (run == null) {
            return;
        }

        try {
            executeRun(run);
        } finally {
            repository.release(run, clock.instant());
        }
    }

    private void executeRun(QualityRun run) {
        long started = System.nanoTime();
        Stage stage = Stage.READ;

        try {
            var rows = readWindow(run);

            stage = Stage.EVALUATE;
            var result = QualityEvaluator.evaluate(run.configuration(), rows);
            if (System.nanoTime() - started > Duration.ofSeconds(properties.timeoutSeconds()).toNanos()) {
                throw new MeasurementReadLimitException(MeasurementReadLimitException.Code.DEADLINE);
            }

            stage = Stage.RECORD;
            quality.record(run, result);

            if (result.output() != null) {
                stage = Stage.OUTPUT;
                writeOutput(run, result.output(), started);
            }
        } catch (RuntimeException exception) {
            recordFailure(run, stage, exception);
        }
    }

    private Map<TimeSeriesId, List<MeasurementReadRow>> readWindow(QualityRun run) {
        var window = new MeasurementWindow(
                run.startedAt().minusSeconds(run.configuration().lookbackSeconds()), run.startedAt(), null);

        return measurements.readWindow(
                new InternalProducerId(run.profileId()),
                window,
                properties.maximumPoints(),
                Duration.ofSeconds(properties.timeoutSeconds()));
    }

    private void writeOutput(QualityRun run, QualityEvaluator.Output output, long started) {
        measurements.writeOutput(
                new InternalProducerId(run.profileId()),
                new TimeSeriesId(run.configuration().output()),
                output.observedAt(),
                output.value(),
                Duration.ofSeconds(properties.timeoutSeconds()).minusNanos(System.nanoTime() - started));

        repository.output(run.id(), QualityRun.OutputState.WRITTEN, null);
    }

    private void recordFailure(QualityRun run, Stage stage, RuntimeException exception) {
        String code = exception instanceof MeasurementReadLimitException limit
                ? limit.code().name() : exception.getClass().getSimpleName();
        LOG.warn("QA run={} stage={} failure={}", run.id(), stage, code);

        // A failed commit may have succeeded server-side. Recovery must preserve committed findings.
        if (stage == Stage.RECORD) {
            throw exception;
        }

        if (stage == Stage.OUTPUT) {
            repository.output(
                    run.id(), QualityRun.OutputState.FAILED, "Derived output failed; acceptance may be uncertain");
        } else {
            quality.fail(run, QualityRun.State.ERROR, stage + ": " + code);
        }
    }

    @Scheduled(fixedDelay = 86_400_000, initialDelay = 120_000, scheduler = "qualityScheduler")
    public void prune() {
        var before = clock.instant().minus(properties.retentionDays(), ChronoUnit.DAYS);
        UUID after = null;

        while (true) {
            var batch = repository.expired(before, after);
            if (batch.isEmpty()) {
                return;
            }

            for (var id : batch) {
                if (!notifications.retainsSource(id)) {
                    repository.deleteRun(id);
                }
            }

            // Keyset progress must include retained runs, or old pending deliveries can starve later pages.
            after = batch.getLast();
        }
    }
}
